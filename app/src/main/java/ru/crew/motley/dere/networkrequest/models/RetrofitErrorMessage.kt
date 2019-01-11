package ru.crew.motley.dere.networkrequest.models

import android.support.annotation.StringRes

/**
 * Created by user28 on 21/3/18.
 */
data class RetrofitErrorMessage(@StringRes val errorResId: Int? = null, val errorMessage: String? = null)