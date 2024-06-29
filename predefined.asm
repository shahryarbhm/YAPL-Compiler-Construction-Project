# MIPS assembler code implementing YAPL predefined functions
# to be used with the MARS simulator
# $Id$
.data
__EOL:
	.asciiz	"\n"
__TRUE:
	.asciiz "True"
__FALSE:
	.asciiz "False"

.text
# void writeint(int i)
.globl writeint
writeint:
    # prolog
    addi	$sp, $sp, -8
    sw		$fp, 8($sp)
    sw		$ra, 4($sp)
    addi	$fp, $sp, 8
    # print_int syscall
    lw		$a0, 4($fp)		# load arg1
    li		$v0, 1			# print_int
    syscall
    # epilog
    lw		$ra, 4($sp)
    lw		$fp, 8($sp)
    addi	$sp, $sp, 8
    jr		$ra
	
# void writebool(bool b)
.globl writebool
writebool:
    # prolog
    addi	$sp, $sp, -8
    sw		$fp, 8($sp)
    sw		$ra, 4($sp)
    addi	$fp, $sp, 8
    # print_int syscall
    lw		$a0, 4($fp)		# load arg1
    beq		$a0, $zero, LL1
    la		$a0, __TRUE
    j		LL2
LL1:
	la		$a0, __FALSE
LL2:
    li		$v0, 4			# print_str
    syscall
    # epilog
    lw		$ra, 4($sp)
    lw		$fp, 8($sp)
    addi	$sp, $sp, 8
    jr		$ra
	
# void writeln()
.globl writeln
writeln:
    # prolog
    addi	$sp, $sp, -8
    sw		$fp, 8($sp)
    sw		$ra, 4($sp)
    addi	$fp, $sp, 8
    la		$a0, __EOL
    li		$v0, 4			# print_str
    syscall
    # epilog
    lw		$ra, 4($sp)
    lw		$fp, 8($sp)
    addi	$sp, $sp, 8
    jr		$ra

# void write(char *s)
.globl write
write:
    # prolog
    addi	$sp, $sp, -8
    sw		$fp, 8($sp)
    sw		$ra, 4($sp)
    addi	$fp, $sp, 8
    lw		$a0, 4($fp)		# load arg1
    li		$v0, 4			# print_str
    syscall
    # epilog
    lw		$ra, 4($sp)
    lw		$fp, 8($sp)
    addi	$sp, $sp, 8
    jr		$ra

# int readint()
.globl readint
readint:
    # prolog
    addi	$sp, $sp, -8
    sw		$fp, 8($sp)
    sw		$ra, 4($sp)
    addi	$fp, $sp, 8
    li		$v0, 5			# read_int
    syscall
    # epilog
    lw		$ra, 4($sp)
    lw		$fp, 8($sp)
    addi	$sp, $sp, 8
    jr		$ra
	
# End of predefined functions
