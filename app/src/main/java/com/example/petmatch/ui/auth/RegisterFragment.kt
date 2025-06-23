package com.example.petmatch.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.petmatch.MainActivity
import com.example.petmatch.R
import com.example.petmatch.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import android.text.InputType
import android.view.MotionEvent
import androidx.core.content.ContextCompat

class RegisterFragment : Fragment(R.layout.fragment_register) {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentRegisterBinding.bind(view)
        auth = FirebaseAuth.getInstance()

        // Mostrar/ocultar contraseña en etPassword
        var isPasswordVisible = false
        binding.etPassword.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = 2
                val drawable = binding.etPassword.compoundDrawables[drawableEnd]
                val drawableWidth = drawable?.bounds?.width() ?: 0
                if (event.rawX >= (binding.etPassword.right - drawableWidth).toFloat()) {
                    isPasswordVisible = !isPasswordVisible
                    if (isPasswordVisible) {
                        binding.etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        binding.etPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_off, 0)
                    } else {
                        binding.etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        binding.etPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0)
                    }
                    binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
                    return@setOnTouchListener true
                }
            }
            false
        }

        // Mostrar/ocultar contraseña en etPasswordConfirm
        var isPasswordConfirmVisible = false
        binding.etPasswordConfirm.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = 2
                val drawable = binding.etPasswordConfirm.compoundDrawables[drawableEnd]
                val drawableWidth = drawable?.bounds?.width() ?: 0
                if (event.rawX >= (binding.etPasswordConfirm.right - drawableWidth).toFloat()) {
                    isPasswordConfirmVisible = !isPasswordConfirmVisible
                    if (isPasswordConfirmVisible) {
                        binding.etPasswordConfirm.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        binding.etPasswordConfirm.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_off, 0)
                    } else {
                        binding.etPasswordConfirm.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        binding.etPasswordConfirm.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0)
                    }
                    binding.etPasswordConfirm.setSelection(binding.etPasswordConfirm.text?.length ?: 0)
                    return@setOnTouchListener true
                }
            }
            false
        }

        binding.btnRegister.setOnClickListener {
            val email    = binding.etEmail.text.toString().trim()
            val pass     = binding.etPassword.text.toString().trim()
            val passConf = binding.etPasswordConfirm.text.toString().trim()

            if (email.isEmpty() || pass.length < 6 || pass != passConf) {
                Toast.makeText(requireContext(), "Verifica tus datos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener {
                    // Tras registro, vamos al MainActivity
                    startActivity(Intent(requireContext(), MainActivity::class.java))
                    requireActivity().finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        binding.tvGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}