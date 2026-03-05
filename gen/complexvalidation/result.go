package complexvalidation

import "fmt"

// ComplexValidationResult represents a cross-field validation error.
// Implements the error interface for use with errors.As.
type ComplexValidationResult struct {
	RuleID      string
	Field       string
	Constraint  string
	Description string
}

func (r *ComplexValidationResult) Error() string {
	return fmt.Sprintf("%s: %s (%s)", r.RuleID, r.Description, r.Field)
}
