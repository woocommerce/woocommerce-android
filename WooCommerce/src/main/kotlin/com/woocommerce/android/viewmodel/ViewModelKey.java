package com.woocommerce.android.viewmodel;

import java.lang.annotation.*;

import androidx.lifecycle.ViewModel;
import dagger.MapKey;

@MapKey
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ViewModelKey {
    Class<? extends ViewModel> value();
}
