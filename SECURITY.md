# Security Policy

## Supported version

Only the latest revision of the `main` branch receives security fixes. This is a portfolio
project and is not currently offered as a hosted production service.

## Reporting a vulnerability

Please use GitHub's **Private vulnerability reporting** feature instead of opening a public
issue. Include the affected endpoint or component, reproduction steps, impact, and any
suggested mitigation.

Do not include real credentials, personal data, access tokens, or destructive proof-of-concept
payloads in a public issue.

## Security scope

The project demonstrates secure defaults, authentication and authorization, input validation,
dependency monitoring, CodeQL analysis, and SBOM generation. HTTP Basic authentication is
intended for demonstration and must only be used over TLS outside localhost. A production
deployment should use a managed identity provider and short-lived tokens.
