package com.woocommerce.android.experimentation.experiment

import com.automattic.android.experimentation.ExPlat
import com.automattic.android.experimentation.Experiment
import javax.inject.Inject

class TestExperiment @Inject constructor(exPlat: ExPlat) : Experiment(
    name = "test",
    exPlat = exPlat
)
