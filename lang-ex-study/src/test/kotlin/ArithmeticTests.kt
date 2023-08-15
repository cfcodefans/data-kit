import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import tool.arithmetic_v1BaseVisitor
import tool.arithmetic_v1Lexer
import tool.arithmetic_v1Parser
import tool.arithmetic_v1Parser.*
import java.math.BigDecimal
import kotlin.test.Test

open class ExprVisitor : arithmetic_v1BaseVisitor<String>() {
    companion object {
        val log: Logger = LogManager.getLogger(ExprVisitor::class.java)
    }

    override fun visitUnary(ctx: arithmetic_v1Parser.UnaryContext?): String {
        val sign: TerminalNode? = ctx!!.sign()?.getChild(TerminalNode::class.java, 0)
        return when (sign?.text) {
            "+" -> visit(ctx.unary())
            "-" -> "-" + visit(ctx.unary())
            "!" -> "!" + visit(ctx.unary())
            null -> visit(ctx.primary())
            else -> visit(ctx.primary())
        }
    }

    override fun visitPrimary(ctx: arithmetic_v1Parser.PrimaryContext?): String {
        return ctx?.NUMBER()?.text
            ?: ctx?.variable()?.VARIABLE()?.text
            ?: visit(ctx?.expr())
    }

    override fun visitExpr(ctx: arithmetic_v1Parser.ExprContext?): String {
        return ctx?.unary()?.let { visit(ctx?.unary()) }
            ?: ctx?.getChild(1)?.let { pt ->
                val left: BigDecimal = visit(ctx.expr(0)).toBigDecimal()
                val right: BigDecimal = visit(ctx.expr(1)).toBigDecimal()
                when (pt) {
                    is TerminalNode -> {
                        when (pt.symbol.type) {
                            PLUS -> left + right
                            MINUS -> left - right
                            TIMES -> left * right
                            DIV -> left / right
                            POW -> left.pow(right.toInt())
                            MOD -> left.toDouble() % right.toDouble()
                            else -> null
                        }
                    }
                    is RelopContext -> {
                        when ((pt.getChild(1) as TerminalNode).symbol.type) {
                            EQ -> left == right
                            GT -> left > right
                            LT -> left < right
                            GEQ -> left >= right
                            LEQ -> left <= right
                            else -> null
                        }
                    }
                    else -> null
                }
            }.toString()

    }
}

open class ArithmeticTests {
    companion object {
        val log: Logger = LogManager.getLogger(ArithmeticTests::class.java)
    }

    @Test
    fun testLogging() {
        log.info("log info")
    }

    @Test
    fun testAntlrExample() = try {
        val input: CharStream = CharStreams.fromString("12     *      -1")
        val lexer: arithmetic_v1Lexer = arithmetic_v1Lexer(input)
        val tokens: CommonTokenStream = CommonTokenStream(lexer)
        val parser: arithmetic_v1Parser = arithmetic_v1Parser(tokens)
        val tree: ParseTree = parser.expr()
        val eval: ExprVisitor = ExprVisitor()
        val result: String = eval.visit(tree)
        log.info("result: $result")
    } catch (t: Throwable) {
        log.error("wtf", t)
    }
}