package org.junit.contrib.java.lang.system;

import static java.lang.String.format;
import static java.lang.System.err;
import static java.lang.System.setErr;
import static java.lang.System.setProperty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.junit.contrib.java.lang.system.Executor.executeFailingTestWithRule;
import static org.junit.contrib.java.lang.system.Executor.executeTestWithRule;
import static org.junit.contrib.java.lang.system.Statements.writeTextToSystemErr;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;

public class SystemErrRuleTest {
	private final PrintStream originalErr = err;

	@Rule
	public TestRule restoreSystemProperties = new RestoreSystemProperties();

	@After
	public void restoreSystemErr() {
		setErr(originalErr);
	}

	@Test
	public void after_the_test_system_err_is_same_as_before() {
		SystemErrRule rule = new SystemErrRule();
		executeTestWithRule(new Statement() {
			@Override
			public void evaluate() throws Throwable {
				PrintStream otherErr = new PrintStream(
					new ByteArrayOutputStream());
				setErr(otherErr);
			}
		}, rule);
		assertThat(err).isSameAs(originalErr);
	}

	@Test
	public void text_is_still_written_to_system_err_by_default() {
		ByteArrayOutputStream systemErr = useReadableSystemErr();
		SystemErrRule rule = new SystemErrRule();
		executeTestWithRule(writeTextToSystemErr("arbitrary text"), rule);
		assertThat(systemErr.toString()).isEqualTo("arbitrary text");
	}

	@Test
	public void no_text_is_written_to_system_err_if_muted_globally() {
		ByteArrayOutputStream systemErr = useReadableSystemErr();
		SystemErrRule rule = new SystemErrRule().mute();
		executeTestWithRule(writeTextToSystemErr("arbitrary text"), rule);
		assertThat(systemErr.toString()).isEmpty();
	}

	@Test
	public void no_text_is_written_to_system_err_after_muted_locally() {
		ByteArrayOutputStream systemErr = useReadableSystemErr();
		final SystemErrRule rule = new SystemErrRule();
		executeTestWithRule(new Statement() {
			@Override
			public void evaluate() throws Throwable {
				err.print("text before muting");
				rule.mute();
				err.print("text after muting");
			}
		}, rule);
		assertThat(systemErr.toString()).isEqualTo("text before muting");
	}

	@Test
	public void no_text_is_written_to_system_err_for_successful_test_if_muted_globally_for_successful_tests() {
		ByteArrayOutputStream systemErr = useReadableSystemErr();
		SystemErrRule rule = new SystemErrRule().muteForSuccessfulTests();
		executeTestWithRule(writeTextToSystemErr("arbitrary text"), rule);
		assertThat(systemErr.toString()).isEmpty();
	}

	@Test
	public void text_is_written_to_system_err_for_failing_test_if_muted_globally_for_successful_tests() {
		ByteArrayOutputStream systemErr = useReadableSystemErr();
		SystemErrRule rule = new SystemErrRule().muteForSuccessfulTests();
		executeFailingTestWithRule(new Statement() {
			@Override
			public void evaluate() throws Throwable {
				err.print("arbitrary text");
				fail();
			}
		}, rule);
		assertThat(systemErr.toString()).isEqualTo("arbitrary text");
	}

	@Test
	public void no_text_is_written_to_system_err_for_successful_test_if_muted_locally_for_successful_tests() {
		ByteArrayOutputStream systemErr = useReadableSystemErr();
		final SystemErrRule rule = new SystemErrRule();
		executeTestWithRule(new Statement() {
			@Override
			public void evaluate() throws Throwable {
				rule.muteForSuccessfulTests();
				err.print("arbitrary text");
			}
		}, rule);
		assertThat(systemErr.toString()).isEmpty();
	}

	@Test
	public void text_is_written_to_system_err_for_failing_test_if_muted_locally_for_successful_tests() {
		ByteArrayOutputStream systemErr = useReadableSystemErr();
		final SystemErrRule rule = new SystemErrRule();
		executeFailingTestWithRule(new Statement() {
			@Override
			public void evaluate() throws Throwable {
				rule.muteForSuccessfulTests();
				err.print("arbitrary text");
				fail();
			}
		}, rule);
		assertThat(systemErr.toString()).isEqualTo("arbitrary text");
	}

	@Test
	public void no_text_is_logged_by_default() {
		SystemErrRule rule = new SystemErrRule();
		executeTestWithRule(writeTextToSystemErr("arbitrary text"), rule);
		assertThat(rule.getLog()).isEmpty();
	}

	@Test
	public void text_is_logged_if_log_has_been_enabled_globally() {
		SystemErrRule rule = new SystemErrRule().enableLog();
		executeTestWithRule(writeTextToSystemErr("arbitrary text"), rule);
		assertThat(rule.getLog()).isEqualTo("arbitrary text");
	}

	@Test
	public void text_is_logged_after_log_has_been_enabled_locally() {
		final SystemErrRule rule = new SystemErrRule();
		executeTestWithRule(new Statement() {
			@Override
			public void evaluate() throws Throwable {
				err.print("text before enabling log");
				rule.enableLog();
				err.print("arbitrary text");
			}
		}, rule);
		assertThat(rule.getLog()).isEqualTo("arbitrary text");
	}

	@Test
	public void log_contains_only_text_that_has_been_written_after_log_was_cleared() {
		final SystemErrRule rule = new SystemErrRule().enableLog();
		executeTestWithRule(new Statement() {
			@Override
			public void evaluate() throws Throwable {
				err.print("text that is cleared");
				rule.clearLog();
				err.print("arbitrary text");
			}
		}, rule);
		assertThat(rule.getLog()).isEqualTo("arbitrary text");
	}

	@Test
	public void text_is_logged_if_rule_is_enabled_and_muted() {
		SystemErrRule rule = new SystemErrRule().enableLog().mute();
		executeTestWithRule(writeTextToSystemErr("arbitrary text"), rule);
		assertThat(rule.getLog()).isEqualTo("arbitrary text");
	}

	@Test
	public void log_is_provided_with_new_line_characters_only_if_requested() {
		setProperty("line.separator", "\r\n");
		SystemErrRule rule = new SystemErrRule().enableLog();
		executeTestWithRule(
			writeTextToSystemErr(format("arbitrary%ntext%n")),
			rule);
		assertThat(rule.getLogWithNormalizedLineSeparator())
			.isEqualTo("arbitrary\ntext\n");
	}

	private ByteArrayOutputStream useReadableSystemErr() {
		ByteArrayOutputStream readableStream = new ByteArrayOutputStream();
		setErr(new PrintStream(readableStream));
		return readableStream;
	}
}
