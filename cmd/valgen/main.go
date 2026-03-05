package main

import (
	"bytes"
	"encoding/json"
	"flag"
	"fmt"
	"go/ast"
	"go/format"
	"go/parser"
	"go/token"
	"log"
	"os"
	"reflect"
	"strings"
	"text/template"
)

// validation-rules.json structures
type Rules struct {
	Rules []Rule `json:"rules"`
}

type Rule struct {
	ID          string    `json:"id"`
	Description string    `json:"description"`
	Condition   Condition `json:"condition"`
	Effect      Effect    `json:"effect"`
	Structure   string    `json:"structure"`
}

type Condition struct {
	Path     string      `json:"path,omitempty"`
	Operator string      `json:"operator,omitempty"`
	Value    interface{} `json:"value,omitempty"`
	All      []Condition `json:"all,omitempty"`
	Any      []Condition `json:"any,omitempty"`
}

type Effect struct {
	Target     string      `json:"target"`
	Constraint string      `json:"constraint"`
	Params     interface{} `json:"params,omitempty"`
}

// Go struct field info extracted from AST
type StructInfo struct {
	Name   string
	Fields []FieldInfo
}

type FieldInfo struct {
	Name    string // Go field name
	JSONTag string // json tag name
	Type    string // Go type string
	IsPtr   bool
}

// Resolved path info for code generation
type ResolvedPath struct {
	AccessChain []PathSegment
}

type PathSegment struct {
	FieldName string
	IsPtr     bool
}

func (r ResolvedPath) GoAccess() string {
	parts := make([]string, len(r.AccessChain))
	for i, seg := range r.AccessChain {
		parts[i] = seg.FieldName
	}
	return "v." + strings.Join(parts, ".")
}

func (r ResolvedPath) NilChecks() []string {
	var checks []string
	for i, seg := range r.AccessChain {
		if !seg.IsPtr {
			continue
		}
		parts := make([]string, i+1)
		for j := 0; j <= i; j++ {
			parts[j] = r.AccessChain[j].FieldName
		}
		checks = append(checks, "v."+strings.Join(parts, "."))
	}
	return checks
}

func main() {
	rulesFile := flag.String("rules", "", "path to validation-rules.json")
	goFile := flag.String("src", "", "path to generated Go source file")
	pkg := flag.String("pkg", "validation", "output package name")
	importPath := flag.String("import", "", "import path for the source package")
	srcPkg := flag.String("src-pkg", "", "source package name (qualifier for types)")
	output := flag.String("o", "", "output file path")
	flag.Parse()

	if *rulesFile == "" || *goFile == "" || *output == "" || *importPath == "" || *srcPkg == "" {
		flag.Usage()
		os.Exit(1)
	}

	rules := loadRules(*rulesFile)
	structs := parseGoFile(*goFile)

	code := generate(rules, structs, *pkg, *importPath, *srcPkg)

	if err := os.WriteFile(*output, []byte(code), 0644); err != nil {
		log.Fatalf("write output: %v", err)
	}
}

func loadRules(path string) Rules {
	data, err := os.ReadFile(path)
	if err != nil {
		log.Fatalf("read rules: %v", err)
	}
	var r Rules
	if err := json.Unmarshal(data, &r); err != nil {
		log.Fatalf("parse rules: %v", err)
	}
	return r
}

func parseGoFile(path string) map[string]*StructInfo {
	fset := token.NewFileSet()
	f, err := parser.ParseFile(fset, path, nil, parser.ParseComments)
	if err != nil {
		log.Fatalf("parse go file: %v", err)
	}

	structs := make(map[string]*StructInfo)
	for _, decl := range f.Decls {
		genDecl, ok := decl.(*ast.GenDecl)
		if !ok || genDecl.Tok != token.TYPE {
			continue
		}
		for _, spec := range genDecl.Specs {
			ts := spec.(*ast.TypeSpec)
			st, ok := ts.Type.(*ast.StructType)
			if !ok {
				continue
			}
			info := &StructInfo{Name: ts.Name.Name}
			for _, field := range st.Fields.List {
				if len(field.Names) == 0 {
					continue
				}
				fi := FieldInfo{Name: field.Names[0].Name}
				if field.Tag != nil {
					fi.JSONTag = extractJSONTag(field.Tag.Value)
				}
				fi.Type, fi.IsPtr = typeString(field.Type)
				info.Fields = append(info.Fields, fi)
			}
			structs[ts.Name.Name] = info
		}
	}
	return structs
}

func extractJSONTag(raw string) string {
	tag := reflect.StructTag(strings.Trim(raw, "`"))
	v, ok := tag.Lookup("json")
	if !ok {
		return ""
	}
	name, _, _ := strings.Cut(v, ",")
	return name
}

func typeString(expr ast.Expr) (string, bool) {
	switch t := expr.(type) {
	case *ast.StarExpr:
		s, _ := typeString(t.X)
		return "*" + s, true
	case *ast.Ident:
		return t.Name, false
	case *ast.SelectorExpr:
		pkg, _ := typeString(t.X)
		return pkg + "." + t.Sel.Name, false
	case *ast.ArrayType:
		elem, _ := typeString(t.Elt)
		return "[]" + elem, false
	default:
		return "interface{}", false
	}
}

// findStruct finds a struct by Smithy name (case-insensitive match)
func findStruct(structs map[string]*StructInfo, smithyName string) *StructInfo {
	lower := strings.ToLower(smithyName)
	for name, info := range structs {
		if strings.ToLower(name) == lower {
			return info
		}
	}
	return nil
}

// resolvePath resolves $.geo.address to Go field access chain
func resolvePath(structs map[string]*StructInfo, rootStruct *StructInfo, path string) (*ResolvedPath, error) {
	path = strings.TrimPrefix(path, "$.")
	parts := strings.Split(path, ".")

	resolved := &ResolvedPath{}
	current := rootStruct

	for _, part := range parts {
		field := findFieldByJSON(current, part)
		if field == nil {
			return nil, fmt.Errorf("field %q not found in %s", part, current.Name)
		}
		resolved.AccessChain = append(resolved.AccessChain, PathSegment{
			FieldName: field.Name,
			IsPtr:     field.IsPtr,
		})
		// Navigate into nested struct
		typeName := strings.TrimPrefix(field.Type, "*")
		if next, ok := structs[typeName]; ok {
			current = next
		}
	}
	return resolved, nil
}

func findFieldByJSON(info *StructInfo, jsonName string) *FieldInfo {
	for i := range info.Fields {
		if info.Fields[i].JSONTag == jsonName {
			return &info.Fields[i]
		}
	}
	return nil
}

// Template data types
type tmplData struct {
	Package    string
	ImportPath string
	SrcPkg     string
	Funcs      []tmplFunc
}

type tmplFunc struct {
	StructName  string
	RuleID      string
	Description string
	Condition   tmplCondition
	Effect      tmplEffect
}

type tmplCondition struct {
	NilChecks []string
	Access    string
	Operator  string
}

type tmplEffect struct {
	NilChecks  []string
	Access     string
	Constraint string
}

var codeTmpl = template.Must(template.New("").Parse(`// Code generated by valgen. DO NOT EDIT.
package {{.Package}}

import (
	"{{.ImportPath}}"

	cv "github.com/imishinist/smithy-poc/gen/complexvalidation"
)
{{range $fn := .Funcs}}
// {{$fn.Description}}
func Validate_{{$fn.RuleID}}(v *{{$.SrcPkg}}.{{$fn.StructName}}) error {
{{- range $fn.Condition.NilChecks}}
	if {{.}} == nil {
		return nil
	}
{{- end}}
{{- if eq $fn.Condition.Operator "not_empty"}}
	if {{$fn.Condition.Access}} == "" {
		return nil
	}
{{- else if eq $fn.Condition.Operator "empty"}}
	if {{$fn.Condition.Access}} != "" {
		return nil
	}
{{- end}}
{{- range $fn.Effect.NilChecks}}
	if {{.}} == nil {
		return &cv.ComplexValidationResult{
			RuleID:      "{{$fn.RuleID}}",
			Field:       "{{$fn.Effect.Access}}",
			Constraint:  "{{$fn.Effect.Constraint}}",
			Description: "{{$fn.Description}}",
		}
	}
{{- end}}
	return nil
}
{{end}}`))

func generate(rules Rules, structs map[string]*StructInfo, pkg, importPath, srcPkg string) string {
	data := tmplData{
		Package:    pkg,
		ImportPath: importPath,
		SrcPkg:     srcPkg,
	}

	for _, rule := range rules.Rules {
		rootStruct := findStruct(structs, rule.Structure)
		if rootStruct == nil {
			log.Fatalf("struct %q not found in Go source", rule.Structure)
		}

		condPath, err := resolvePath(structs, rootStruct, rule.Condition.Path)
		if err != nil {
			log.Fatalf("resolve condition path %q: %v", rule.Condition.Path, err)
		}

		effectPath, err := resolvePath(structs, rootStruct, rule.Effect.Target)
		if err != nil {
			log.Fatalf("resolve effect path %q: %v", rule.Effect.Target, err)
		}

		// Remove effect nil checks already covered by condition
		condChecks := make(map[string]bool)
		for _, c := range condPath.NilChecks() {
			condChecks[c] = true
		}
		var effectNilChecks []string
		for _, c := range effectPath.NilChecks() {
			if !condChecks[c] {
				effectNilChecks = append(effectNilChecks, c)
			}
		}

		data.Funcs = append(data.Funcs, tmplFunc{
			StructName:  rootStruct.Name,
			RuleID:      rule.ID,
			Description: rule.Description,
			Condition: tmplCondition{
				NilChecks: condPath.NilChecks(),
				Access:    condPath.GoAccess(),
				Operator:  rule.Condition.Operator,
			},
			Effect: tmplEffect{
				NilChecks:  effectNilChecks,
				Access:     effectPath.GoAccess(),
				Constraint: rule.Effect.Constraint,
			},
		})
	}

	var buf bytes.Buffer
	if err := codeTmpl.Execute(&buf, data); err != nil {
		log.Fatalf("execute template: %v", err)
	}

	formatted, err := format.Source(buf.Bytes())
	if err != nil {
		log.Printf("warning: gofmt failed: %v, using unformatted output", err)
		return buf.String()
	}
	return string(formatted)
}
