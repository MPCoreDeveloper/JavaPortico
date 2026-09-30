package io.github.mpcoredeveloper.javaportico;

import io.github.mpcoredeveloper.javaportico.GeneratorTestDriver.RunResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contracts that cannot compile are refused with a reason, rather than emitted as code the consumer has to
 * diagnose from a compiler error inside generated source.
 * Ported from SharpPortico's {@code ContractRefusalTests}.
 */
class ContractRefusalTests {

    /**
     * A document the reader cannot parse is refused with the reader's own words.
     *
     * <p>The shape below is the one that cost a real session: a property whose value is a plain scalar where a
     * mapping belongs, seen here as a tab where YAML allows only spaces. The reader reports it and hands back a
     * document without an info section - and answering "missing info/title section" sends the caller to inspect
     * the one part of the file that is fine, for as long as it takes to read the whole contract by hand.
     * </p>
     */
    @Test
    void aDocumentThatFailsToParseIsRefusedWithTheReadersOwnComplaint() {
        String spec = String.join("\n",
                "openapi: 3.0.3",
                "info: { title: Broken, version: 1.0.0 }",
                "components:",
                "\tschemas:",
                "    Thing:",
                "      type: object",
                "      properties:",
                "        text: { type: string }");

        RunResult result = GeneratorTestDriver.tryRun(spec, "BrokenService");

        assertFalse(result.isSuccess(), "a document the reader refused must not be emitted");
        assertNull(result.proto());
        assertNull(result.proxy());
        assertFalse(result.diagnostics().isEmpty(), "a refusal must say why");

        String refusal = String.join("\n", result.diagnostics());
        assertFalse(refusal.contains("missing info/title section"),
                () -> "the reader named the fault, so the refusal should carry its words: " + refusal);
    }

    /**
     * An enumeration whose generated name is already taken by a different member set is refused.
     *
     * <p>{@code StateEnumPending} is declared, and the inline {@code state} of {@code Refund} derives that same
     * name - so two different lifecycles would share one generated type, silently. The other honest answer is
     * generating a third name, but the contract is the only place that can say which of the two the name means.
     * </p>
     */
    @Test
    void aDerivedEnumerationNameThatIsAlreadyTakenIsRefused() {
        String spec = String.join("\n",
                "openapi: 3.0.3",
                "info: { title: Enums, version: 1.0.0 }",
                "paths:",
                "  /refunds:",
                "    get:",
                "      operationId: ListRefunds",
                "      responses:",
                "        '200':",
                "          description: ok",
                "          content:",
                "            application/json:",
                "              schema: { $ref: '#/components/schemas/Refund' }",
                "components:",
                "  schemas:",
                "    StateEnumPending:",
                "      type: string",
                "      enum: [queued]",
                "    Artifact:",
                "      type: object",
                "      properties:",
                "        state: { type: string, enum: [pending, committed] }",
                "    Refund:",
                "      type: object",
                "      properties:",
                "        state: { type: string, enum: [pending] }");

        RunResult result = GeneratorTestDriver.tryRun(spec, "EnumService");

        assertFalse(result.isSuccess(), "two member sets behind one generated name must not be emitted");
        assertNull(result.proto());
        assertNull(result.proxy());

        String refusal = String.join("\n", result.diagnostics());
        assertTrue(refusal.contains("StateEnumPending"), () -> refusal);
        assertTrue(refusal.contains("Queued") && refusal.contains("Pending"),
                () -> "the refusal names both member sets: " + refusal);
        assertNotNull(result.diagnostics().get(0));
    }
}
