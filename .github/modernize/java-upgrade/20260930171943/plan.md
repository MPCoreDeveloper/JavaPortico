# Upgrade Plan: JavaPortico (20260930171943)

- **Generated**: 2026-09-30 17:19:43
- **HEAD Branch**: main
- **HEAD Commit ID**: N/A

## Available Tools

**JDKs**
- JDK 21: not available (baseline will be skipped)
- JDK 25.0.4.1: C:\Users\Posse\.tools\jdk25\jdk-25.0.4.1+1\bin (used by upgrade and final validation)

**Build Tools**
- Maven 3.9.11: C:\Users\Posse\.tools\apache-maven-3.9.11\bin

## Guidelines

- Upgrade Java runtime to the latest LTS version.
- Execute in auto-execution mode.

> Note: You can add any specific guidelines or constraints for the upgrade process here if needed, bullet points are preferred.

## Options

- Working branch: appmod/java-upgrade-20260930171943
- Run tests before and after the upgrade: true

## Upgrade Goals

- Java runtime and build target to latest LTS (Java 25) across all modules.

## Technology Stack

| Technology/Dependency | Current | Min Compatible Version | Why Incompatible |
| --------------------- | ------- | ---------------------- | ---------------- |
| Java (root modules) | 25 | 25 | User requested latest LTS; already aligned |
| Java (samples/spring-boot-example) | 21 | 25 | Module-specific override prevents full-repo Java 25 alignment |
| Maven | 3.9.11 | 3.9.0 | Already compatible with Java 25 |
| maven-compiler-plugin | 3.15.0 | 3.11.0 | Already compatible |
| maven-surefire-plugin | 3.5.6 | 3.0.0 | Already compatible |
| Spring Boot sample BOM | 3.5.16 | 3.5.x | Already compatible with Java 25 baseline tooling |

## Derived Upgrades

- Align module-level compiler release override in `samples/spring-boot-example/pom.xml` from 21 to 25 so all modules target the user-requested latest LTS runtime.
- Validate the Spring Boot sample against Java 25 after removing the stale compatibility rationale that references JDK 24 ASM limits.
- Keep Maven 3.9.11 and current compiler/surefire plugin versions unchanged because they already satisfy Java 25 compatibility.

## Impact Analysis

### Dependency Changes

| File | Dependency | Current | Action | Target | Reason |
|------|-----------|---------|--------|--------|--------|
| samples/spring-boot-example/pom.xml | maven.compiler.release | 21 | upgrade | 25 | Full repository Java runtime alignment to latest LTS |

### Source Code Changes

| File | Location | Current | Required Change | Reason |
|------|----------|---------|----------------|--------|
| samples/spring-boot-example/pom.xml | properties comment above `maven.compiler.release` | States module must stay at release 21 due to ASM/JDK 24 limit | Remove/replace with neutral comment for Java 25 alignment | Avoid stale rationale conflicting with requested upgrade goal |

### Configuration Changes

No configuration property migrations required.

### CI/CD Changes

| File | Location | Current | Required Change |
|------|----------|---------|----------------|
| .github/workflows/ci.yml | Java setup matrix | `25` | No change required |
| .github/workflows/publish.yml | Java setup version | `25` | No change required |

### Risks & Warnings

- **Spring framework bytecode parser assumptions in sample module**: Prior comment states Java 25 classfiles were not supported. **Mitigation**: run full `mvn clean test-compile` and `mvn clean test` using JDK 25 at final validation; if failure appears, update Spring Boot/Spring Framework line to a compatible patch release in the same major line.

## Upgrade Steps

- Step 1: Setup Environment
  - **Rationale**: Confirm required JDK/build tool availability before any upgrade action.
  - **Changes to Make**: None (tools already present).
  - **Verification**: Command: `appmod-list-jdks` and `appmod-list-mavens`; JDK: N/A; Expected Result: JDK 25 and Maven 3.9.11 available.

- Step 2: Setup Baseline
  - **Rationale**: Capture before-upgrade baseline on current project JDK.
  - **Changes to Make**: None.
  - **Verification**: Command: `mvn clean compile test-compile -q && mvn clean test -q`; JDK: 21; Expected Result: baseline pass rate captured. (Will be skipped if JDK 21 unavailable.)

- Step 3: Align Spring Boot Sample to Java 25
  - **Rationale**: Remove the only remaining Java 21 compiler target to satisfy latest-LTS goal consistently.
  - **Changes to Make**: Apply Dependency Changes row and Source Code Changes row in Impact Analysis.
  - **Verification**: Command: `mvn clean test-compile -q`; JDK: 25; Expected Result: compilation success across modules and tests.

- Step 4: CVE Validation & Fix
  - **Rationale**: Ensure upgraded dependency set has no known direct-dependency CVE issues.
  - **Changes to Make**: Run CVE scan; apply minimal patch upgrades only if reported.
  - **Verification**: Command: `mvn dependency:list -DexcludeTransitive=true` + CVE scan tool + `mvn clean test-compile -q`; JDK: 25; Expected Result: CVEs fixed or documented as no patch available.

- Step 5: Final Validation
  - **Rationale**: Enforce success criteria and runtime target completion.
  - **Changes to Make**: Resolve any TODOs/workarounds and fix all failing tests.
  - **Verification**: Command: `mvn clean test-compile -q` then `mvn clean test -q`; JDK: 25; Expected Result: compile success and 100% test pass rate (or at least baseline if baseline existed).
