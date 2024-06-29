.text
.globl _allocArray
_allocArray:
    addi	$sp, $sp, -8
    sw  	$fp, 8($sp)
    addi	$fp, $sp, 8
    sw  	$ra, -4($fp)
    addi	$sp, $sp, -4	# a (offset = -8)
    addi	$sp, $sp, -4	# i (offset = -12)
    lw  	$8, 4($30)
    lw  	$9, 8($30)
    addi	$9, $9, 1
    sll 	$9, $9, 2
    add 	$8, $8, $9
    lw  	$8, 0($8)
    sw  	$8, 0($23)
    addi	$sp, $sp, -4	# saveRegs
    sw  	$8, -16($fp)
    lw  	$4, 0($23)
    addi	$4, $4, 1
    sll 	$4, $4, 2
    li  	$2, 9
    syscall		# sbrk
    lw  	$4, 0($23)
    sw  	$4, 0($2)
    move	$8, $v0
    addi	$sp, $sp, 4	# restoreRegs complete
    la  	$9, -8($30)
    sw  	$8, 0($9)
    lw  	$8, 8($30)
    li  	$9, 1
    add 	$8, $8, $9
    la  	$9, 8($30)
    sw  	$8, 0($9)
    lw  	$8, 8($30)
    lw  	$9, 12($30)
    slt 	$8, $8, $9
    beqz	$8, LR0
    li  	$8, 0
    la  	$9, -12($30)
    sw  	$8, 0($9)
LR1:
    lw  	$8, -12($30)
    lw  	$9, -8($30)
    lw  	$9, 0($9)
    slt 	$8, $8, $9
    beqz	$8, LR2
    lw  	$8, -8($30)
    lw  	$9, -12($30)
    addi	$9, $9, 1
    sll 	$9, $9, 2
    add 	$8, $8, $9
    lw  	$9, 4($30)
    lw  	$10, 8($30)
    lw  	$11, 12($30)
    addi	$sp, $sp, -28	# saveRegs
    sw  	$8, -16($fp)
    sw  	$9, -20($fp)
    sw  	$10, -24($fp)
    sw  	$11, -28($fp)
    sw  	$9, 4($sp)	# arg 0
    sw  	$10, 8($sp)	# arg 1
    sw  	$11, 12($sp)	# arg 2
    jal 	_allocArray
    add 	$9, $v0, $zero
    lw  	$8, -16($fp)
    addi	$sp, $sp, 28	# restoreRegs complete
    sw  	$9, 0($8)
    lw  	$8, -12($30)
    li  	$9, 1
    add 	$8, $8, $9
    la  	$9, -12($30)
    sw  	$8, 0($9)
    j   	LR1
LR2:
    j   	LR3
LR0:
LR3:
    lw  	$8, -8($30)
    move	$v0, $8
    j   	_allocArray_ret
_allocArray_ret:
    lw  	$ra, -4($fp)
    move	$sp, $fp
    lw  	$fp, 0($fp)
    jr  	$ra
