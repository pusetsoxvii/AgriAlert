package com.example.agrialert.ui.viewmodels;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.agrialert.model.User;
import com.example.agrialert.repository.UserRepository;

public class AuthViewModel extends AndroidViewModel {

    private UserRepository repository;

    private MutableLiveData<User> _loginSuccess = new MutableLiveData<>();
    public LiveData<User> getLoginSuccess() { return _loginSuccess; }

    private MutableLiveData<String> _loginError = new MutableLiveData<>();
    public LiveData<String> getLoginError() { return _loginError; }

    private MutableLiveData<Boolean> _registerSuccess = new MutableLiveData<>();
    public LiveData<Boolean> getRegisterSuccess() { return _registerSuccess; }

    private MutableLiveData<String> _registerError = new MutableLiveData<>();
    public LiveData<String> getRegisterError() { return _registerError; }

    public AuthViewModel(Application application) {
        super(application);
        repository = new UserRepository(application);
    }

    public void login(String email, String password) {
        User user = repository.login(email, password);
        if (user != null) {
            _loginSuccess.setValue(user);
        } else {
            _loginError.setValue("Invalid email or password");
        }
    }

    public void register(String name, String email, String password, String role) {
        boolean success = repository.register(name, email, password, role);
        if (success) {
            _registerSuccess.setValue(true);
        } else {
            _registerError.setValue("Registration failed or email already exists");
        }
    }
}
