# MARS MIPS Macros Reference

> A practical Markdown guide to **macros, `.eqv`, and `.include` in the MARS MIPS assembler**.
>
> **Source:** [MARS — Writing and Using Macros](https://dpetersanderson.github.io/Help/MacrosHelp.html), from the MARS documentation. This guide reorganizes and paraphrases the material, with original examples.
>
> **Version:** These three directives were introduced in **MARS 4.3**. They are assembler features, not MIPS machine instructions.

## Contents

- [Quick reference](#quick-reference)
- [What macros do](#what-macros-do)
- [Defining a macro](#defining-a-macro)
- [Calling a macro](#calling-a-macro)
- [Parameters and token substitution](#parameters-and-token-substitution)
- [Rules and limitations](#rules-and-limitations)
- [Reusable macro examples](#reusable-macro-examples)
- [Local labels and macro expansion](#local-labels-and-macro-expansion)
- [The `.eqv` directive](#the-eqv-directive)
- [The `.include` directive](#the-include-directive)
- [Complete two-file example](#complete-two-file-example)
- [Understanding source line numbers](#understanding-source-line-numbers)
- [Common pitfalls](#common-pitfalls)
- [Macro vs. procedure vs. `.eqv`](#macro-vs-procedure-vs-eqv)

## Quick reference

| Syntax | Purpose | Example |
|:--|:--|:--|
| `.macro name` | Start a parameterless macro definition. | `.macro quit` |
| `.macro name (%arg1, %arg2)` | Start a parameterized definition. | `.macro swap (%r1, %r2)` |
| `.end_macro` | End the current macro definition. | `.end_macro` |
| `name` | Expand a parameterless macro. | `quit` |
| `name (arg1, arg2)` | Expand a macro with arguments. | `swap ($s0, $s1)` |
| `.eqv NAME replacement` | Define a text substitution for later uses. | `.eqv EXIT_SERVICE 10` |
| `.include "filename"` | Insert another source file during preprocessing. | `.include "macros.asm"` |

**Core idea:** A macro inserts its assembly-language body at each call site *during assembly*. It is **not** a runtime subroutine call.

## What macros do

A macro gives a name to an often-used sequence of assembly instructions. Each invocation expands into its instructions before the program runs.

For example, instead of repeatedly writing:

```mips
li $v0, 10
syscall
```

Define a macro once:

```mips
.macro quit
    li $v0, 10
    syscall
.end_macro
```

Then use it wherever appropriate:

```mips
quit
```

The assembler substitutes the two instructions for `quit`. Unlike `jal`, a macro invocation does not transfer control to a separate procedure or require a return instruction. However, repeated expansion also increases the amount of generated code.

## Defining a macro

### General form

```mips
.macro macro_name (%first, %second)
    # Assembly statements using %first and %second
.end_macro
```

- Start with `.macro` and the macro's name.
- Declare zero or more **formal parameters** if needed.
- Write the statements that will be expanded when the macro is invoked.
- Finish with `.end_macro`.
- Formal parameter names ordinarily begin with `%`. MARS also accepts `$`-prefixed macro parameter names for SPIM-style compatibility.
- Parentheses and commas around/in formal parameter lists are optional.
- A macro body may use both `.text` and `.data` segments.

### Example: a macro with one parameter

```mips
.macro print_constant (%number)
    li $a0, %number
    li $v0, 1
    syscall
.end_macro

.text
main:
    print_constant (42)
```

This particular macro expects an **immediate integer** because it places `%number` in the operand position of `li`.

## Calling a macro

A macro call supplies the macro name and one argument per declared parameter.

```mips
.macro copy_value (%destination, %source)
    move %destination, %source
.end_macro

copy_value ($s0, $t0)
```

MARS allows multiple spellings for the invocation, including:

```mips
copy_value ($s0, $t0)
copy_value $s0, $t0
copy_value $s0 $t0
```

These calls have the same intent. Comma separation and surrounding parentheses are optional. Consistently using `name (arg1, arg2)` usually makes code easier to read.

## Parameters and token substitution

A macro argument is substituted **textually** wherever its matching parameter appears in the body. MARS does not automatically convert between a register, an immediate, an address, or a label.

### Registers versus immediate values

```mips
# Expects a register such as $s0.
.macro print_register (%reg)
    move $a0, %reg
    li   $v0, 1
    syscall
.end_macro

# Expects a numeric immediate such as 123.
.macro print_literal (%value)
    li   $a0, %value
    li   $v0, 1
    syscall
.end_macro

print_register ($s0)
print_literal (123)
```

Do not assume `print_literal ($s0)` or `print_register (123)` will work: the substituted operand must still be valid for the instruction receiving it.

### Single-token argument restriction

Each actual argument must be **one assembler token**. For instance, a complete base-plus-offset address such as `8($sp)` is not accepted as one macro argument.

Prefer separating address components:

```mips
.macro load_stack_word (%destination, %offset)
    lw %destination, %offset($sp)
.end_macro

load_stack_word ($t1, 8)
```

The macro constructs the address operand from the offset parameter and `$sp` in the body.

## Rules and limitations

| Rule | Effect |
|:--|:--|
| **Define before use** | A macro cannot be invoked before its definition has been processed. |
| **File-local definitions** | Macro definitions are not exported globally between source files. Use `.include` to make common definitions available. |
| **No nested definitions** | Do not put a `.macro` declaration inside another macro's body. |
| **Backward macro references only** | One macro can call another only if the called macro was defined earlier. |
| **Overloading by parameter count** | The same macro name can identify different macros if the number of parameters differs. |
| **Duplicate name and arity** | If another macro has the same name **and** parameter count, the later definition is ignored. |
| **One token per argument** | Compound expressions such as `8($sp)` cannot be passed directly as a single argument. |
| **Local labels are renamed** | Labels inside an expanded macro get a unique suffix to prevent collisions. |
| **Assembler-dependent** | Macro syntax is not part of the MIPS instruction set; another assembler may behave differently. |

## Reusable macro examples

### 1. Print a newline

```mips
.macro newline
    li $a0, 10       # ASCII line feed
    li $v0, 11       # Print character
    syscall
.end_macro

newline
```

### 2. Print an integer stored in a register

```mips
.macro show_integer (%register)
    move $a0, %register
    li   $v0, 1
    syscall
.end_macro

li $s0, 275
show_integer ($s0)
```

### 3. Print a string literal

A macro can switch to a data segment, create storage, then return to the text segment.

```mips
.macro show_message (%message)
    .data
message_label: .asciiz %message
    .text
    li $v0, 4
    la $a0, message_label
    syscall
.end_macro

show_message ("First message\n")
show_message ("Second message\n")
```

Each expansion gets a different version of `message_label`, so the two string declarations do not collide.

### 4. Swap two register values

```mips
# Uses $at? No: this macro uses $t9 as scratch storage.
# Caller must be willing to have $t9 overwritten.
.macro swap_registers (%left, %right)
    move $t9, %left
    move %left, %right
    move %right, $t9
.end_macro

swap_registers ($s0, $s1)
```

**Register caution:** Expanded code executes with ordinary MIPS register side effects. Macros do not automatically save and restore scratch registers.

### 5. Repeat a body macro a fixed number of times

This example illustrates a macro that invokes a **previously defined, parameterless** macro.

```mips
.macro print_iteration
    li $v0, 1
    move $a0, $t0
    syscall
.end_macro

.macro repeat_count (%counter, %limit, %action)
    li %counter, 0
repeat_here:
    %action ()
    addi %counter, %counter, 1
    blt %counter, %limit, repeat_here
.end_macro

# Prints 01234 with no separators.
repeat_count ($t0, 5, print_iteration)
```

The example uses `$t0` as its counter and clobbers `$v0` and `$a0`. Its `%limit` argument is an immediate here; for a general-purpose loop, consider register usage and termination conditions carefully.

## Local labels and macro expansion

When a macro has an internal label, MARS rewrites the label name in each expansion by appending a unique identifier such as `_M0` or `_M1`.

```mips
.macro spin_once (%register)
spin_label:
    addi %register, %register, -1
    bgtz %register, spin_label
.end_macro

li $t0, 3
spin_once ($t0)

li $t1, 2
spin_once ($t1)
```

Conceptually, the assembler creates distinct labels similar to:

```text
spin_label_M0:
spin_label_M1:
```

This permits repeated uses of a macro with internal branches without label redefinition errors.

## The `.eqv` directive

`.eqv` defines an identifier that will be replaced with a specified text fragment during assembly preprocessing. It is analogous to a simple C/C++ preprocessor definition but independent of the macro mechanism.

### General form

```mips
.eqv IDENTIFIER replacement_text
```

### Defining constants and register aliases

```mips
.eqv PRINT_INT 1
.eqv MAX_ITEMS 12
.eqv INDEX_REG $t3

li   INDEX_REG, 0
li   $v0, PRINT_INT
move $a0, INDEX_REG
syscall
```

After substitution, `INDEX_REG` becomes `$t3` and `PRINT_INT` becomes `1`.

### Substituting an instruction

The replacement need not be just one token:

```mips
.eqv ZERO_INDEX add $t3, $zero, $zero

ZERO_INDEX
```

The assembler processes that occurrence as:

```mips
add $t3, $zero, $zero
```

**Key facts:**

- Define an `.eqv` name **before** referring to it.
- An `.eqv` definition is **local to its source file**, unless brought into another file with `.include`.
- A macro body can refer to previously declared `.eqv` names.
- `.eqv` replaces an identifier with arbitrary text; it does not define a parameterized macro.

## The `.include` directive

`.include` brings the contents of another assembly file into the current source during assembly preprocessing, similarly to C/C++ `#include`.

### Syntax

```mips
.include "macros.asm"
```

Typical uses include sharing macro definitions, `.eqv` aliases, and other assembly source across multiple programs.

### Important behaviors

- The operand is a **quoted filename**.
- The included file's contents are substituted at the directive's location.
- Definitions intended for the rest of the program should appear before they are used; place `.include` near the top when appropriate.
- MARS detects **circular includes** (direct or indirect) and reports them.
- Errors inside included files identify that file and its own source line when possible.
- Line numbers shown in the Text Segment can jump between source files because they refer back to original source positions.

## Complete two-file example

Place these files together in the same directory.

### File 1: `macros.asm`

```mips
# Shared definitions for MARS programs.
.eqv SERVICE_PRINT_INT 1
.eqv SERVICE_PRINT_CHAR 11
.eqv SERVICE_EXIT 10

.macro output_int (%register)
    move $a0, %register
    li $v0, SERVICE_PRINT_INT
    syscall
.end_macro

.macro output_newline
    li $a0, 10
    li $v0, SERVICE_PRINT_CHAR
    syscall
.end_macro

.macro finish_program
    li $v0, SERVICE_EXIT
    syscall
.end_macro
```

### File 2: `main.asm`

```mips
.include "macros.asm"

.text
.globl main
main:
    li $s0, 2026
    output_int ($s0)
    output_newline
    finish_program
```

**Expected console output:**

```text
2026
```

MARS first incorporates `macros.asm` into the source being assembled, then expands calls such as `output_int ($s0)` at their call sites. Runtime execution sees only the resulting instructions.

## Understanding source line numbers

Macro expansion can involve **two relevant locations**: where the macro was invoked and where the expanded statement was originally defined.

| Display form | Interpretation |
|:--|:--|
| `24->6` | A diagnostic involving macro code: **24** identifies the expansion/use location and **6** the line of the macro definition. |
| `<6>` | A definition-source line shown at an expansion point in the Text Segment source display. |

The exact numbers vary with your files; they correspond to lines in the original source editor. Included files may similarly cause the displayed source line numbers to move backward or forward.

## Common pitfalls

| Symptom | Likely cause | Fix |
|:--|:--|:--|
| Unknown macro name | The macro appears later in the file or is never included. | Move the definition or `.include` before the call. |
| Operand/type error after expansion | Argument text is invalid for an instruction (e.g., a register passed where `li` needs an immediate). | Check each parameter's operand position. |
| Unexpected macro body | Two definitions share the same name and number of parameters. | Use distinct names or distinct arities. |
| Failure passing `4($sp)` | Arguments must each be one token. | Pass the offset and base separately or construct the address in the body. |
| Labels seem to change | MARS gives labels in macro bodies distinct suffixes. | This is expected; inspect the expanded source if necessary. |
| Register unexpectedly changed | Macro instructions write ordinary registers. | Document clobbered registers; save/restore where needed. |
| Circular include error | Files include one another in a cycle. | Remove the include cycle. |

## Macro vs. procedure vs. `.eqv`

| Feature | Macro (`.macro`) | Procedure (`jal`, `jr`) | Text alias (`.eqv`) |
|:--|:--|:--|:--|
| Expanded at assembly time | Yes | No | Yes |
| Supports parameters | Yes, by token substitution | Via calling convention/registers/stack | No formal parameters |
| Separate runtime call/return | No | Yes | No |
| Repeated uses duplicate instruction body | Yes | Typically no | Depends on replacement text |
| Runtime call/return overhead | None | Present | None |
| Best suited to | Small reusable instruction patterns | Larger shared behavior | Constants, aliases, short replacements |

**Rule of thumb:** Choose `.eqv` for straightforward aliases, a macro for a compact parameterized instruction pattern, and a procedure when reusable runtime code should have a single implementation.

---

*Based on the [MARS Macros Help page](https://dpetersanderson.github.io/Help/MacrosHelp.html). The original MARS macro feature was contributed by Mohammad Hossein Sekhavat; `.eqv` and `.include` were added by Pete Sanderson. For assembler-specific edge cases, the upstream documentation is authoritative.*
