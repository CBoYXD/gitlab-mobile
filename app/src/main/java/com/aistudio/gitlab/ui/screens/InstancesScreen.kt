package com.aistudio.gitlab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.gitlab.data.model.GitLabInstance
import com.aistudio.gitlab.ui.components.GitLabTanukiIcon
import com.aistudio.gitlab.ui.theme.GitLabDanger
import com.aistudio.gitlab.ui.theme.GitLabOrange
import com.aistudio.gitlab.ui.theme.GitLabPurple
import com.aistudio.gitlab.ui.theme.GitLabSuccess
import com.aistudio.gitlab.ui.viewmodel.ConnectionTestState

@Composable
fun InstancesScreen(
  activeInstance: GitLabInstance?,
  savedInstances: List<GitLabInstance>,
  connectionTestState: ConnectionTestState,
  onSwitchInstance: (String) -> Unit,
  onAddInstance: (name: String, url: String, token: String, makeActive: Boolean) -> Unit,
  onDeleteInstance: (String) -> Unit,
  onTestConnection: (url: String, token: String) -> Unit,
  onClearTestState: () -> Unit,
  modifier: Modifier = Modifier,
  onBack: (() -> Unit)? = null
) {
  var showAddDialog by remember { mutableStateOf(false) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          onClearTestState()
          showAddDialog = true
        },
        containerColor = GitLabOrange,
        contentColor = Color.White,
        modifier = Modifier.testTag("add_instance_fab")
      ) {
        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Custom Instance")
      }
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp)
    ) {
      // Header Title
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (onBack != null) {
            IconButton(
              onClick = onBack,
              modifier = Modifier.padding(end = 8.dp)
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
              )
            }
          }
          Column {
            Text(
              text = "GitLab Instances",
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Switch or connect to any self-hosted or cloud GitLab server",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      // Active Instance Hero Card
      item {
        Text(
          text = "CURRENT ACTIVE INSTANCE",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = GitLabOrange,
          modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
        )

        Card(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("active_instance_card"),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, GitLabOrange)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                GitLabTanukiIcon(size = 28.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = activeInstance?.name ?: "No server connected",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                  )
                  Text(
                    text = activeInstance?.url?.takeIf { it.isNotBlank() } ?: "Add a GitLab server",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              val online = activeInstance?.statusOk == true
              Surface(
                color = (if (online) GitLabSuccess else GitLabDanger).copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .size(6.dp)
                      .clip(CircleShape)
                      .background(if (online) GitLabSuccess else GitLabDanger)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = if (online) "Connected" else "Offline",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (online) GitLabSuccess else GitLabDanger
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              InstanceMetadataPill(title = "Version", value = activeInstance?.version?.ifBlank { "—" } ?: "—")
              InstanceMetadataPill(
                title = "Auth",
                value = when {
                  activeInstance == null -> "None"
                  activeInstance.token.isNotBlank() -> "PAT"
                  else -> "Public"
                }
              )
              InstanceMetadataPill(
                title = "Type",
                value = when {
                  activeInstance == null -> "—"
                  activeInstance.isCustom -> "Self-hosted"
                  else -> "GitLab.com"
                }
              )
            }
          }
        }
      }

      // Saved Instances List
      item {
        Spacer(modifier = Modifier.height(20.dp))
        Text(
          text = "SAVED INSTANCES (${savedInstances.size})",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(bottom = 8.dp)
        )
      }

      items(savedInstances, key = { it.id }) { instance ->
        InstanceItemCard(
          instance = instance,
          isActive = instance.id == activeInstance?.id,
          onSelect = { onSwitchInstance(instance.id) },
          onDelete = { onDeleteInstance(instance.id) }
        )
        Spacer(modifier = Modifier.height(8.dp))
      }

      // Documentation / Help Tip Card
      item {
        Spacer(modifier = Modifier.height(16.dp))
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
          ) {
            Icon(
              imageVector = Icons.Default.HelpOutline,
              contentDescription = null,
              tint = GitLabOrange,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Connecting to Private GitLab Instances",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "For corporate or self-hosted servers (Omnibus, Docker, K8s), generate a Personal Access Token in GitLab: User Settings → Access Tokens. Check scopes: api and read_user.",
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
              )
            }
          }
        }
      }
    }
  }

  if (showAddDialog) {
    AddCustomInstanceDialog(
      testState = connectionTestState,
      onDismiss = { showAddDialog = false },
      onTest = onTestConnection,
      onSave = { name, url, token, makeActive ->
        onAddInstance(name, url, token, makeActive)
        showAddDialog = false
      }
    )
  }
}

@Composable
fun InstanceItemCard(
  instance: GitLabInstance,
  isActive: Boolean,
  onSelect: () -> Unit,
  onDelete: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onSelect)
      .testTag("instance_row_${instance.id}"),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
      else MaterialTheme.colorScheme.surface
    ),
    border = androidx.compose.foundation.BorderStroke(
      width = if (isActive) 1.5.dp else 1.dp,
      color = if (isActive) GitLabOrange else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        RadioButton(
          selected = isActive,
          onClick = onSelect,
          colors = RadioButtonDefaults.colors(selectedColor = GitLabOrange)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = instance.name,
              fontWeight = FontWeight.Bold,
              fontSize = 14.5.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            if (instance.isCustom) {
              Spacer(modifier = Modifier.width(6.dp))
              Surface(
                color = GitLabPurple.copy(alpha = 0.12f),
                shape = RoundedCornerShape(4.dp)
              ) {
                Text(
                  text = "Custom",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = GitLabPurple,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
              }
            }
          }
          Text(
            text = instance.url,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      if (instance.isCustom) {
        IconButton(
          onClick = onDelete,
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Remove",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

@Composable
fun InstanceMetadataPill(title: String, value: String) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant,
    shape = RoundedCornerShape(6.dp)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "$title: ",
        fontSize = 10.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = value,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}

@Composable
fun AddCustomInstanceDialog(
  testState: ConnectionTestState,
  onDismiss: () -> Unit,
  onTest: (url: String, token: String) -> Unit,
  onSave: (name: String, url: String, token: String, makeActive: Boolean) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var url by remember { mutableStateOf("https://") }
  var token by remember { mutableStateOf("") }
  var tokenVisible by remember { mutableStateOf(false) }
  var makeActive by remember { mutableStateOf(true) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        GitLabTanukiIcon(size = 24.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Connect Custom GitLab", fontWeight = FontWeight.Bold, fontSize = 17.sp)
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Instance Name") },
          placeholder = { Text("Company GitLab") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("instance_name_input")
        )

        OutlinedTextField(
          value = url,
          onValueChange = { url = it },
          label = { Text("Server URL") },
          placeholder = { Text("https://gitlab.com") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("instance_url_input")
        )

        OutlinedTextField(
          value = token,
          onValueChange = { token = it },
          label = { Text("Personal Access Token (PAT)") },
          placeholder = { Text("glpat-xxxx... (Optional for public)") },
          visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
          trailingIcon = {
            IconButton(onClick = { tokenVisible = !tokenVisible }) {
              Icon(
                imageVector = if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = null
              )
            }
          },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("instance_token_input")
        )

        // Test Connection Button & Status Feedback
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          OutlinedButton(
            onClick = { onTest(url, token) },
            enabled = url.isNotBlank() && !testState.isTesting,
            modifier = Modifier.testTag("test_connection_button")
          ) {
            if (testState.isTesting) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Testing...", fontSize = 12.sp)
            } else {
              Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Test Server", fontSize = 12.sp)
            }
          }

          if (testState.message != null) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f).padding(start = 8.dp)
            ) {
              Icon(
                imageVector = if (testState.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (testState.isSuccess) GitLabSuccess else GitLabDanger,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = testState.message,
                fontSize = 11.sp,
                color = if (testState.isSuccess) GitLabSuccess else GitLabDanger,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(name.ifBlank { url.removePrefix("https://").removePrefix("http://") }, url, token, makeActive) },
        enabled = url.isNotBlank() && url != "https://",
        colors = ButtonDefaults.buttonColors(containerColor = GitLabOrange),
        modifier = Modifier.testTag("save_instance_button")
      ) {
        Text("Save & Connect")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}
