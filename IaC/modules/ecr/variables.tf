variable "project" {
  description = "Project name"
  type        = string
}

variable "environment" {
  description = "Deployment environment (dev, qc, pdn)"
  type        = string
}

variable "image_tag_mutability" {
  description = "Tag mutability setting: MUTABLE or IMMUTABLE. Use IMMUTABLE in pdn for image integrity."
  type        = string
  default     = "MUTABLE"
}

variable "scan_on_push" {
  description = "Enable image vulnerability scanning on push"
  type        = bool
  default     = true
}

variable "keep_last_n_images" {
  description = "Number of tagged images to retain (lifecycle policy)"
  type        = number
  default     = 5
}
