# MARS MIPS Syscall Reference

> A practical, Markdown-formatted reference for the **MARS MIPS simulator**.
>
> **Source:** [MARS — SYSCALL functions](https://dpetersanderson.github.io/Help/SyscallHelp.html) by Pete Sanderson. This guide reorganizes and paraphrases the original documentation.

## Contents

- [Quick start](#quick-start)
- [Syscalls at a glance](#syscalls-at-a-glance)
- [Console output](#console-output)
- [Console input](#console-input)
- [Memory and program termination](#memory-and-program-termination)
- [File operations](#file-operations)
- [Time, delays, and MIDI](#time-delays-and-midi)
- [Pseudorandom numbers](#pseudorandom-numbers)
- [GUI dialogs](#gui-dialogs)
- [Important behavior and edge cases](#important-behavior-and-edge-cases)
- [Code examples](#code-examples)
- [MIDI details](#midi-details)

## Quick start

To invoke a MARS system service:

1. Put the **syscall number** in `$v0`.
2. Put any inputs into the required registers (commonly `$a0`–`$a3` or `$f12`).
3. Execute `syscall`.
4. Read any outputs from the specified registers.

```mips
# Print the integer stored in $t0
li   $v0, 1
move $a0, $t0
syscall
```

> **Compatibility:** Services **1–17** generally work in SPIM too, with some differences for opening files (`13`). Services **30 and above are MARS-specific**. Other than documented output registers, syscall calls preserve register values.

## Syscalls at a glance

| Group | Purpose | Syscall numbers |
|:--|:--|:--|
| Basic I/O | Printing and keyboard input | `1`–`8`, `11`–`12` |
| Memory / process | Heap allocation and exit | `9`–`10`, `17` |
| Files | Open, read, write, close | `13`–`16` |
| Time / sound | Clock, MIDI, sleep | `30`–`33` |
| Number display | Hexadecimal, binary, unsigned | `34`–`36` |
| Random | Seeds and random values | `40`–`44` |
| GUI | Input and message dialogs | `50`–`59` |

Unused syscall numbers in the documented ranges: **`37`–`39` and `45`–`49`**.

## Console output

| Code (`$v0`) | Service | Inputs | Output / behavior |
|---:|---|---|---|
| `1` | Print integer | `$a0`: signed integer | Writes an integer in decimal. |
| `2` | Print float | `$f12`: float | Writes a floating-point value. |
| `3` | Print double | `$f12`: double | Writes a double-precision value. |
| `4` | Print string | `$a0`: pointer to null-terminated text | Writes the string. |
| `11` | Print character | `$a0`: character value | Writes the ASCII character from the least significant byte. |
| `34` | Print hexadecimal | `$a0`: integer | Writes **8 hexadecimal digits**, zero-padded. |
| `35` | Print binary | `$a0`: integer | Writes **32 binary digits**, zero-padded. |
| `36` | Print unsigned | `$a0`: integer bit pattern | Writes its unsigned decimal interpretation. |

## Console input

| Code (`$v0`) | Service | Inputs | Return value |
|---:|---|---|---|
| `5` | Read integer | — | `$v0`: integer entered. |
| `6` | Read float | — | `$f0`: float entered. |
| `7` | Read double | — | `$f0`: double entered. |
| `8` | Read string | `$a0`: buffer address; `$a1`: buffer capacity `n` | Stores text in the provided buffer; see [string-input rules](#string-input-syscall-8). |
| `12` | Read character | — | `$v0`: character read. |

### String-input rules (syscall `8`)

This service behaves similarly to C's `fgets`:

- With capacity `n`, there is room for at most **`n - 1` text characters** plus a null terminator.
- If a shorter line is entered, its newline is retained when it fits; the buffer is null-terminated.
- When `n = 1`, the buffer receives only a null byte.
- When `n < 1`, the buffer is not changed.

## Memory and program termination

| Code (`$v0`) | Service | Inputs | Return value / behavior |
|---:|---|---|---|
| `9` | Allocate heap (`sbrk`) | `$a0`: number of bytes | `$v0`: starting address of the allocated region. |
| `10` | Exit | — | Ends execution. |
| `17` | Exit with status | `$a0`: exit status | Ends execution with a status value; **MARS's GUI ignores the status**. |

## File operations

| Code (`$v0`) | Service | Inputs | Return value / behavior |
|---:|---|---|---|
| `13` | Open file | `$a0`: filename pointer; `$a1`: flags; `$a2`: mode (ignored) | `$v0`: file descriptor, or negative on error. |
| `14` | Read file | `$a0`: descriptor; `$a1`: destination buffer; `$a2`: maximum byte count | `$v0`: bytes read; `0` means EOF, negative means error. |
| `15` | Write file | `$a0`: descriptor; `$a1`: source buffer; `$a2`: byte count | `$v0`: bytes written, or negative on error. |
| `16` | Close file | `$a0`: descriptor | Closes the specified descriptor. |

### File-opening flags (syscall `13`)

| Flag (`$a1`) | Meaning |
|---:|---|
| `0` | Open for reading. |
| `1` | Open for writing, creating the file if necessary. |
| `9` | Open for appending, creating the file if necessary. |

`$a2` (mode) is accepted but **ignored** by MARS.

Standard descriptors are predefined:

| Descriptor | Stream |
|---:|---|
| `0` | Standard input |
| `1` | Standard output |
| `2` | Standard error |

Newly opened files receive descriptors starting at **`3`**. In MARS version 3.7 and later, services `13`–`15` return results in **`$v0`**, not `$a0`.

## Time, delays, and MIDI

| Code (`$v0`) | Service | Inputs | Return value / behavior |
|---:|---|---|---|
| `30` | System time | — | `$a0`: low 32 bits; `$a1`: high 32 bits of milliseconds since the Unix epoch. |
| `31` | MIDI output | `$a0`: pitch; `$a1`: duration (ms); `$a2`: instrument; `$a3`: volume | Starts a tone and **returns immediately**. |
| `32` | Sleep | `$a0`: milliseconds | Pauses the simulation thread for at least approximately the given interval; actual timing may be longer. |
| `33` | MIDI output (synchronous) | Same four arguments as `31` | Plays a tone and **returns after its duration**. |

See [MIDI details](#midi-details) for parameter ranges and defaults.

## Pseudorandom numbers

Each generator is identified by an integer in `$a0`. Generators use Java's `java.util.Random` internally; use syscall `40` to obtain repeatable sequences.

| Code (`$v0`) | Service | Inputs | Return value / behavior |
|---:|---|---|---|
| `40` | Set random seed | `$a0`: generator ID; `$a1`: seed | Sets that generator's seed. |
| `41` | Random integer | `$a0`: generator ID | `$a0`: random signed 32-bit integer. |
| `42` | Random integer below bound | `$a0`: generator ID; `$a1`: positive exclusive upper bound | `$a0`: integer in **`[0, upper_bound)`**. |
| `43` | Random float | `$a0`: generator ID | `$f0`: float in **`[0.0, 1.0)`**. |
| `44` | Random double | `$a0`: generator ID | `$f0`: double in **`[0.0, 1.0)`**. |

> **Watch the return register:** `41` and `42` return their answer in `$a0`, overwriting the generator ID that was there. `43` and `44` return their result in `$f0`.

## GUI dialogs

These services display dialogs through the MARS graphical environment.

| Code (`$v0`) | Dialog | Inputs | Outputs |
|---:|---|---|---|
| `50` | Confirm | `$a0`: message string address | `$a0`: choice (`0` Yes, `1` No, `2` Cancel). |
| `51` | Input integer | `$a0`: prompt string address | `$a0`: integer; `$a1`: status. |
| `52` | Input float | `$a0`: prompt string address | `$f0`: float; `$a1`: status. |
| `53` | Input double | `$a0`: prompt string address | `$f0`: double; `$a1`: status. |
| `54` | Input string | `$a0`: prompt; `$a1`: destination buffer; `$a2`: capacity | Buffer receives text; `$a1`: status. |
| `55` | Show message | `$a0`: message; `$a1`: icon type | Displays a message dialog. |
| `56` | Show message + integer | `$a0`: message; `$a1`: integer | Displays a message followed by an integer. |
| `57` | Show message + float | `$a0`: message; `$f12`: float | Displays a message followed by a float. |
| `58` | Show message + double | `$a0`: message; `$f12`: double | Displays a message followed by a double. |
| `59` | Show message + string | `$a0`: first string; `$a1`: second string | Displays both strings together. |

### Numeric input dialog statuses (`51`–`53`)

| `$a1` | Meaning |
|---:|---|
| `0` | Input accepted. |
| `-1` | The input could not be parsed as the expected number. |
| `-2` | User canceled. |
| `-3` | User confirmed an empty field. |

### String input dialog statuses (`54`)

| `$a1` | Meaning |
|---:|---|
| `0` | Input accepted; buffer contains the text. |
| `-2` | Canceled; buffer left unchanged. |
| `-3` | Empty input confirmed; buffer left unchanged. |
| `-4` | Input was too long; buffer holds the maximum allowed text and a trailing null byte. |

String-capacity conventions follow syscall `8`.

### Message dialog icons (`55`)

| `$a1` | Icon |
|---:|---|
| `0` | Error |
| `1` | Information |
| `2` | Warning |
| `3` | Question |
| Other | No icon |

## Important behavior and edge cases

- **The syscall number always goes in `$v0`**, but results are *not always* returned there. Check each row before using a return value.
- **File I/O:** Inspect negative return values from `13`–`15` and check for end-of-file (`0`) when reading.
- **Printing a character (`11`):** Only the low byte of `$a0` is used.
- **Time (`30`):** Combines `$a1` (high half) and `$a0` (low half) to make a 64-bit millisecond count since **January 1, 1970**.
- **MIDI (`31` vs. `33`):** `31` starts playing without waiting; `33` waits until the sound's duration has elapsed.
- **Sleeping (`32`):** The exact delay depends on the underlying Java runtime and is not guaranteed to be precise.
- **Portability:** Avoid MARS-only services (`30+`) in code meant to execute in SPIM or on a real MIPS operating system.

## Code examples

### 1. Print a string and an integer

```mips
.data
message: .asciiz "Answer: "
newline: .asciiz "\n"

.text
.globl main
main:
    li $v0, 4
    la $a0, message
    syscall

    li $v0, 1
    li $a0, 42
    syscall

    li $v0, 4
    la $a0, newline
    syscall

    li $v0, 10
    syscall
```

### 2. Read a line of text

```mips
.data
buffer: .space 64
prompt: .asciiz "Enter a line: "

.text
.globl main
main:
    li $v0, 4
    la $a0, prompt
    syscall

    li $v0, 8
    la $a0, buffer
    li $a1, 64
    syscall

    li $v0, 4
    la $a0, buffer
    syscall

    li $v0, 10
    syscall
```

`buffer` has 64 bytes of capacity, so at most 63 bytes of input text can be stored before the null terminator.

### 3. Create a file, write bytes, and close it

```mips
.data
filename: .asciiz "output.txt"
message:  .ascii  "Hello, MARS!\n"  # Exactly 13 bytes

.text
.globl main
main:
    # Open output.txt for writing.
    li $v0, 13
    la $a0, filename
    li $a1, 1                # Create / write
    li $a2, 0                # Ignored by MARS
    syscall

    bltz $v0, failed         # A negative descriptor signals failure.
    move $s0, $v0

    # Write 13 bytes from message.
    li $v0, 15
    move $a0, $s0
    la $a1, message
    li $a2, 13
    syscall

    # Close the file.
    li $v0, 16
    move $a0, $s0
    syscall

    li $v0, 10
    syscall

failed:
    li $v0, 10
    syscall
```

The file is written relative to the directory from which MARS is running (unless you use an absolute path). In production code, also inspect the result of syscall `15` for short writes or errors.

### 4. Get a random number from `0` to `9`

```mips
li $v0, 42          # Random integer below an upper bound
li $a0, 0           # Generator ID
li $a1, 10          # Upper bound is exclusive
syscall             # $a0 is now 0..9

move $t0, $a0       # Preserve result before another syscall
```

### 5. Show a confirmation dialog

```mips
.data
question: .asciiz "Do you want to continue?"

.text
li $v0, 50
la $a0, question
syscall
# $a0 = 0 (Yes), 1 (No), or 2 (Cancel)
```

## MIDI details

MARS uses Java MIDI synthesis for syscall `31` (nonblocking) and `33` (blocking).

### MIDI arguments

| Register | Parameter | Accepted values | Default if invalid |
|---|---|---|---|
| `$a0` | Pitch | `0`–`127`, in semitone steps | `60` (middle C) |
| `$a1` | Duration | Nonnegative duration in milliseconds | `1000` ms if negative |
| `$a2` | Instrument | `0`–`127` (General MIDI program) | `0` (Acoustic Grand Piano) |
| `$a3` | Volume / velocity | `0`–`127` | `100` |

To raise or lower a note by one octave, add or subtract **12** from its pitch number.

| Pitch | Note | Pitch | Note |
|---:|---|---:|---|
| `60` | C | `66` | F♯ / G♭ |
| `61` | C♯ / D♭ | `67` | G |
| `62` | D | `68` | G♯ / A♭ |
| `63` | D♯ / E♭ | `69` | A |
| `64` | E | `70` | A♯ / B♭ |
| `65` | F | `71` | B |
| | | `72` | C (next octave) |

### General MIDI instrument families

| Program numbers | Family | Program numbers | Family |
|---|---|---|---|
| `0`–`7` | Piano | `64`–`71` | Reed |
| `8`–`15` | Chromatic percussion | `72`–`79` | Pipe |
| `16`–`23` | Organ | `80`–`87` | Synth lead |
| `24`–`31` | Guitar | `88`–`95` | Synth pad |
| `32`–`39` | Bass | `96`–`103` | Synth effects |
| `40`–`47` | Strings | `104`–`111` | Ethnic |
| `48`–`55` | Ensemble | `112`–`119` | Percussion |
| `56`–`63` | Brass | `120`–`127` | Sound effects |

> **MIDI numbering note:** MARS/Java uses instruments numbered **0–127**. Some General MIDI lists use **1–128**, so subtract 1 from numbers in those lists. Sound and timbre depend on the synthesizer installed on the machine.

### Play middle C for half a second

```mips
li $v0, 33      # Wait for tone completion
li $a0, 60      # Middle C
li $a1, 500     # 500 ms
li $a2, 0       # Acoustic Grand Piano
li $a3, 100     # Velocity
syscall
```

---

*Reference based on the [official MARS syscall help page](https://dpetersanderson.github.io/Help/SyscallHelp.html). Register names and service numbers match MARS; descriptions and examples have been rewritten and reorganized for quick reference.*
