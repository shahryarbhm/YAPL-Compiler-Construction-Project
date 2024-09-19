# YAPL Compiler Project

## Overview

The YAPL (Yet Another Programming Language) compiler project involves extending a provided compiler to ensure the correct use of data types in YAPL source programs and to generate MIPS assembler code from YAPL code. This project aims to enhance the functionality and reliability of the YAPL compiler through thorough type checking and efficient code generation.

## Goals

- Investigate and extend the provided YAPL compiler to check the correct use of data types in YAPL source programs.
- Investigate and extend the provided YAPL compiler to generate MIPS assembler code from YAPL source programs.

## Preparation

To get started with the project, please ensure you have access to the following resources available on the Moodle course website:

- **YAPL Type Checking Document**: Guidelines and specifications for implementing type checking.
- **YAPL Arrays and Records Document**: Information regarding the handling of arrays and records within the YAPL language.
- **CA3 Package**: This package contains:
  - A reduced implementation of the YAPL compiler.
  - YAPL test files for both the reduced and full-featured versions of the compiler.
  - An ANT build file (`build-dist-asm.xml`) for facilitating compiler development and testing.

## Testing

To ensure the compiler meets the specified requirements, run the following commands to execute all relevant tests:

### Type Checking Tests

```bash
ant -f build-dist-asm.xml -Dtest-version=typecheck eval-all
```

### Code Generation Tests

```bash
ant -f build-dist-asm.xml -Dtest-version=codegen eval-all
```

These commands will evaluate the compiler's functionality against the provided test sets, ensuring that all expected behaviors are met.
Contributing

Contributions to the YAPL compiler are welcome. Please feel free to fork the repository, make your changes, and submit a pull request for review.
License

This project is licensed under the MIT License.
