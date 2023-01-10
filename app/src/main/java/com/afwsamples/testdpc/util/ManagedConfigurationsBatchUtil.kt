package com.afwsamples.testdpc.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.RestrictionEntry
import android.net.Uri
import com.afwsamples.testdpc.models.RestrictionBatchEntry
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import java.io.*
import java.nio.charset.StandardCharsets

object ManagedConfigurationsBatchUtil {

    private val mGson = GsonBuilder()
        .enableComplexMapKeySerialization()
        .setPrettyPrinting()
        .create()
    private val type = object : TypeToken<List<RestrictionBatchEntry>>() {}.type

    @SuppressLint("NewApi")
    fun exportToJsonFile(
        selectedFile: Uri,
        context: Context,
        restrictions: List<RestrictionEntry>,
        processStateCallBack: ProcessStateCallBack
    ) {
        val restrictionEntries: MutableList<RestrictionBatchEntry> = ArrayList()

        val fos: OutputStream? = context.contentResolver.openOutputStream(selectedFile)
        fos.use {
            val bw = BufferedWriter(OutputStreamWriter(it, StandardCharsets.UTF_8))
            bw.use {
                for (restrictionEntry in restrictions) {
                    restrictionEntries.add(RestrictionBatchEntry(restrictionEntry))
                }
                mGson.toJson(restrictionEntries, bw)
            }
        }
        processStateCallBack.onFinished()
    }

    @SuppressLint("NewApi")
    fun importFromJsonFile(
        selectedFile: Uri,
        context: Context,
        processStateCallBack: ProcessStateCallBack
    ) {
        val restrictionEntries: MutableList<RestrictionEntry> = ArrayList()
        val fis: InputStream? = context.contentResolver.openInputStream(selectedFile)

        fis.use {
            val br = BufferedReader(InputStreamReader(fis, StandardCharsets.UTF_8))
            br.use {
                val restrictionBatchEntries: List<RestrictionBatchEntry> = mGson.fromJson(br, type)
                for (restrictionBatchEntry in restrictionBatchEntries) {
                    val entry = convertToEntry(restrictionBatchEntry)
                    restrictionEntries.add(entry)
                }
                processStateCallBack.onFinished(restrictionEntries)
            }
        }
    }

    @SuppressLint("NewApi")
    private fun convertToEntry(batchEntry: RestrictionBatchEntry): RestrictionEntry {
        val entry = RestrictionEntry(batchEntry.type, batchEntry.key).apply {
            title = batchEntry.title
            description = batchEntry.description
            choiceEntries = batchEntry.choiceEntries
            choiceValues = batchEntry.choiceValues
            selectedString = batchEntry.currentValue
            allSelectedStrings = batchEntry.currentValues
        }

        runBlocking {
            CoroutineScope(Dispatchers.IO).async {
                if (batchEntry.restrictions != null && batchEntry.restrictions.isNotEmpty()) {
                    val restrictionEntries =
                        arrayOfNulls<RestrictionEntry>(batchEntry.restrictions.size)
                    val restrictionBatchEntries = batchEntry.restrictions

                    for (i in batchEntry.restrictions.indices) {
                        restrictionEntries[i] = convertToEntry(restrictionBatchEntries[i])
                    }

                    entry.restrictions = restrictionEntries
                }
            }.await()
        }

        return entry
    }

    fun importFromEmbeddedJson() {}

    abstract class ProcessStateCallBack {
        abstract fun onFinished()

        abstract fun onFinished(list: MutableList<RestrictionEntry>)
    }
}