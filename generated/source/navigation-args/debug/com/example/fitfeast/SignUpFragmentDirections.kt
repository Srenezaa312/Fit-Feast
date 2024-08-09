package com.example.fitfeast

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections

public class SignUpFragmentDirections private constructor() {
  public companion object {
    public fun actionSignUpFragmentToUserProfileCreationFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_signUpFragment_to_userProfileCreationFragment)
  }
}
