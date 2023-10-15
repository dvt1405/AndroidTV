package com.kt.apps.voiceselector.usecase

import org.jsoup.select.Evaluator.IsFirstChild

data class UserHistory(
    val isFirstChild: IsFirstChild
)
class CheckUserHistory {
}