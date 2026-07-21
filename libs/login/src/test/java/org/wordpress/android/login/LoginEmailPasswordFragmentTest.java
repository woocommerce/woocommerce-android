package org.wordpress.android.login;

import org.junit.Test;
import org.wordpress.android.login.LoginWpcomService.LoginState;
import org.wordpress.android.login.LoginWpcomService.LoginStep;

import static org.assertj.core.api.Assertions.assertThat;

public class LoginEmailPasswordFragmentTest {
    private static final String FALLBACK_MESSAGE = "Generic error";

    @Test
    public void givenFailureMessageWhenResolvingFailureMessageThenBackendMessageIsUsed() {
        LoginState state = LoginState.failure("The server rejected this request");

        String message = LoginEmailPasswordFragment.resolveFailureMessage(state, FALLBACK_MESSAGE);

        assertThat(message).isEqualTo("The server rejected this request");
    }

    @Test
    public void givenNullFailureMessageWhenResolvingFailureMessageThenFallbackIsUsed() {
        LoginState state = new LoginState(LoginStep.FAILURE);

        String message = LoginEmailPasswordFragment.resolveFailureMessage(state, FALLBACK_MESSAGE);

        assertThat(message).isEqualTo(FALLBACK_MESSAGE);
    }

    @Test
    public void givenEmptyFailureMessageWhenResolvingFailureMessageThenFallbackIsUsed() {
        LoginState state = LoginState.failure("");

        String message = LoginEmailPasswordFragment.resolveFailureMessage(state, FALLBACK_MESSAGE);

        assertThat(message).isEqualTo(FALLBACK_MESSAGE);
    }

    @Test
    public void givenBlankFailureMessageWhenResolvingFailureMessageThenFallbackIsUsed() {
        LoginState state = LoginState.failure(" \t\n");

        String message = LoginEmailPasswordFragment.resolveFailureMessage(state, FALLBACK_MESSAGE);

        assertThat(message).isEqualTo(FALLBACK_MESSAGE);
    }
}
