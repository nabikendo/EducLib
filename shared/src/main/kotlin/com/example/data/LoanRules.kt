package com.example.data

/*
 * Regras de circulação do EducLib, sem dependência de Android ou banco de dados.
 * Usadas pelo app Android (LibraryRepository) e pelo app de desktop, para que os dois
 * calculem prazos, devoluções e penalidades da mesma forma.
 */

const val DAY_MS: Long = 24L * 60 * 60 * 1000
const val DEFAULT_STUDENT_LOAN_DAYS = 7L
const val DEFAULT_TEACHER_LOAN_DAYS = 15L
const val RENEWAL_DAYS = 7L

/** Texto gravado em `LoanEntity.borrowerClass`, igual ao que o app Android grava. */
fun BorrowerEntity.loanClassLabel(): String =
    if (type == "Aluno") "Aluno - ${classRoom.ifBlank { identifier }}"
    else "Prof - ${classRoom.ifBlank { "Sem Turma" }}"

fun isTeacherLoanClass(borrowerClass: String) = borrowerClass.startsWith("Prof", ignoreCase = true)

fun loanDueDate(borrowerClass: String, loanDate: Long, studentDays: Long, teacherDays: Long): Long =
    loanDate + (if (isTeacherLoanClass(borrowerClass)) teacherDays else studentDays) * DAY_MS

fun BorrowerEntity.isBlocked(now: Long): Boolean = blockedUntil != null && blockedUntil > now

/** Motivo para recusar um empréstimo, ou nulo se pode emprestar. */
fun loanRefusal(book: BookEntity?, quantity: Int, borrower: BorrowerEntity?, activeLoans: List<LoanEntity>, now: Long): String? = when {
    book == null -> "Livro não encontrado."
    quantity <= 0 -> "A quantidade deve ser maior que zero."
    book.availableQuantity < quantity -> "Só há ${book.availableQuantity} exemplar(es) disponível(is) de “${book.title}”."
    borrower != null && borrower.isBlocked(now) -> "${borrower.name} está bloqueado(a) por atraso."
    borrower != null && activeLoans.any { it.borrowerName == borrower.name && it.returnDate == null && now > it.dueDate } ->
        "${borrower.name} tem empréstimo atrasado. Registre a devolução antes de um novo empréstimo."
    else -> null
}

/** Livro depois de emprestar [quantity] exemplares. */
fun BookEntity.afterLoan(quantity: Int, loanDate: Long): BookEntity = copy(
    availableQuantity = availableQuantity - quantity,
    totalLoans = totalLoans + quantity,
    useFrequency = useFrequency + quantity,
    lastLoanDate = loanDate
)

/**
 * Resultado de uma devolução total ou parcial. Na parcial, o empréstimo original fica ativo com o
 * restante e [returnedRecord] guarda no histórico a parte devolvida (com id 0, a ser atribuído).
 */
data class ReturnOutcome(val updatedLoan: LoanEntity, val returnedRecord: LoanEntity?, val returnedQuantity: Int)

fun LoanEntity.applyReturn(requestedQuantity: Int, returnDate: Long): ReturnOutcome? {
    if (this.returnDate != null) return null
    val returned = if (requestedQuantity <= 0 || requestedQuantity >= quantity) quantity else requestedQuantity
    return if (returned == quantity) {
        ReturnOutcome(copy(returnDate = returnDate), null, returned)
    } else {
        ReturnOutcome(copy(quantity = quantity - returned), copy(id = 0, quantity = returned, returnDate = returnDate), returned)
    }
}

fun BookEntity.afterReturn(returnedQuantity: Int): BookEntity =
    copy(availableQuantity = (availableQuantity + returnedQuantity).coerceAtMost(totalQuantity))

/** Novo bloqueio após devolução atrasada (um dia de bloqueio por dia de atraso), ou nulo se não houve atraso. */
fun penaltyBlockedUntil(loan: LoanEntity, returnDate: Long, currentBlockedUntil: Long?, now: Long): Long? {
    if (returnDate <= loan.dueDate) return null
    val overdueDays = (returnDate - loan.dueDate) / DAY_MS
    if (overdueDays <= 0) return null
    return maxOf(currentBlockedUntil ?: now, now) + overdueDays * DAY_MS
}

/**
 * Um livro só pode ser excluído quando ninguém está com ele. Empréstimos devolvidos ficam como
 * histórico (guardam título e autor) e reservas do livro são removidas junto com ele.
 */
fun bookDeletionRefusal(book: BookEntity, loans: List<LoanEntity>): String? {
    val active = loans.filter { it.bookId == book.id && it.returnDate == null }.sumOf { it.quantity }
    return if (active > 0) "“${book.title}” tem $active exemplar(es) emprestado(s). Registre a devolução antes de excluir." else null
}
