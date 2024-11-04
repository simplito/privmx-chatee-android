//
// PrivMX Chatee Android.
// Copyright © 2024 Simplito sp. z o.o.
//
// This file is part of demonstration software for the PrivMX Platform (https://privmx.dev).
// This software is Licensed under the MIT License.
//
// See the License for the specific language governing permissions and
// limitations under the License.
//

package com.simplito.chatee.classes

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts

data class SaveDocumentResult(
    val uri: Uri?,
    val fileId: String,
    val updateDownloadingState: (newValue: Boolean) -> Unit
)

data class SaveDocumentInput(
    val fileName: String,
    val fileId: String,
    val updateDownloadingState: (newValue: Boolean) -> Unit
)

class SaveDocumentStreamResultContract(
    private val mimetype: String,
) : ActivityResultContract<SaveDocumentInput, SaveDocumentResult>() {
    private val createDocument = ActivityResultContracts.CreateDocument(mimetype)
    private var input: SaveDocumentInput? = null
    override fun createIntent(context: Context, input: SaveDocumentInput): Intent {
        this.input = input
        return createDocument.createIntent(context, input.fileName)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): SaveDocumentResult {
        val uri = createDocument.parseResult(resultCode, intent)
        return SaveDocumentResult(uri, input!!.fileId, input!!.updateDownloadingState)
    }
}