# BiteCart Development Rules

1. Build BiteCart incrementally, one phase at a time.
2. Implement ONLY the phase I explicitly authorize.
3. Never implement future phases automatically.
4. Inspect the existing project before making changes.
5. Preserve all working functionality from previous phases.
6. Do not unnecessarily rewrite existing code.
7. Keep package names, class names, URLs, database relationships, and architecture consistent.
8. Keep the project buildable after every phase.
9. Use Java 17+ and Spring Boot 3.x.
10. Use Spring MVC, Spring Data JPA, Hibernate, MySQL, and Thymeleaf.
11. Use BigDecimal for all monetary values.
12. Never store plaintext passwords.
13. Use BCrypt for password hashing.
14. Never hardcode passwords, API keys, or other secrets.
15. Use DTOs appropriately.
16. Use Jakarta Validation.
17. Use appropriate exception handling.
18. Use @Transactional for transactional business operations where required.
19. Prevent cross-user data access and IDOR vulnerabilities.
20. Avoid unnecessary dependencies.
21. Do not introduce Flyway or Liquibase unless explicitly requested.
22. Avoid blindly using CascadeType.ALL.
23. Avoid circular JPA equals/hashCode/toString implementations.
24. Do not blindly use Lombok @Data on JPA entities.
25. Run mvn clean install after every phase.
26. Fix compilation and test failures before declaring a phase complete.
27. At the end of every phase, report files created, files modified, important changes, build/test results, and remaining issues.
28. STOP after the requested phase.
29. Wait for explicit approval before starting the next phase.
30. Never modify database credentials unless explicitly requested.
31. Never expose secrets in source code, logs, responses, or documentation.
