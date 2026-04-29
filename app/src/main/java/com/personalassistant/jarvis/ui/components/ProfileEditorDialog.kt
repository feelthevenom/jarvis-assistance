package com.personalassistant.jarvis.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.personalassistant.jarvis.data.UserProfile

@Composable
fun ProfileEditorDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit,
) {
    var name by remember(profile) { mutableStateOf(profile.name) }
    var age by remember(profile) { mutableStateOf(profile.age) }
    var dateOfBirth by remember(profile) { mutableStateOf(profile.dateOfBirth) }
    var personalDetail by remember(profile) { mutableStateOf(profile.personalDetail) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profile") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ProfileField(value = name, onValueChange = { name = it }, label = "Name")
                Spacer(modifier = Modifier.height(10.dp))
                ProfileField(value = age, onValueChange = { age = it }, label = "Age")
                Spacer(modifier = Modifier.height(10.dp))
                ProfileField(
                    value = dateOfBirth,
                    onValueChange = { dateOfBirth = it },
                    label = "Date of Birth",
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = personalDetail,
                    onValueChange = { personalDetail = it },
                    label = { Text("Personal Detail") },
                    supportingText = {
                        Text("Used as personal context for Thragg when answering you.")
                    },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        UserProfile(
                            name = name.trim(),
                            age = age.trim(),
                            dateOfBirth = dateOfBirth.trim(),
                            personalDetail = personalDetail.trim(),
                        ),
                    )
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun ProfileField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyMedium,
    )
}
