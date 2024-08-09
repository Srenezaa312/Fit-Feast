package com.example.fitfeast

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections

public class SignInFragmentDirections private constructor() {
  public companion object {
    public fun actionSignInFragmentToDashboardFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_signInFragment_to_dashboardFragment)
  }
}
