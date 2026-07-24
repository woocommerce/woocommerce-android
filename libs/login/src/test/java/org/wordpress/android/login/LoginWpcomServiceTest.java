package org.wordpress.android.login;

import org.junit.Test;
import org.wordpress.android.login.LoginWpcomService.LoginState;
import org.wordpress.android.login.LoginWpcomService.LoginStep;

import static org.assertj.core.api.Assertions.assertThat;

public class LoginWpcomServiceTest {
    @Test
    public void givenFailureMessageWhenCreatingFailureStateThenMessageIsAvailableOnFailure() {
        LoginState state = LoginState.failure("The server rejected this request");

        assertThat(state.getStep()).isEqualTo(LoginStep.FAILURE);
        assertThat(state.getFailureMessage()).isEqualTo("The server rejected this request");
        assertThat(state.getClass()).isEqualTo(LoginState.class);
    }

    @Test
    public void givenOrdinaryFailureStateWhenReadingFailureMessageThenMessageIsNull() {
        LoginState state = new LoginState(LoginStep.FAILURE);

        assertThat(state.getFailureMessage()).isNull();
    }
}
