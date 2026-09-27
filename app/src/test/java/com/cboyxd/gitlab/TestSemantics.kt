package com.cboyxd.gitlab

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher

/** Matches a node whose click action carries [label] (Modifier.clickable(onClickLabel = …)). */
fun hasClickLabel(label: String) = SemanticsMatcher("click label = '$label'") {
  it.config.getOrNull(SemanticsActions.OnClick)?.label == label
}
