package com.udacity.project4.locationreminders


import androidx.lifecycle.LiveData
import com.google.firebase.auth.FirebaseUser
import org.mockito.Mockito.mock

class FakeFirebaseUserLiveData() : LiveData<FirebaseUser?>() {

    override fun getValue(): FirebaseUser? {
        return mock(FirebaseUser::class.java)
    }
}