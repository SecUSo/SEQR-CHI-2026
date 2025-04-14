/*
    Privacy Friendly QR Scanner
    Copyright (C) 2017-2025 Privacy Friendly QR Scanner authors and SECUSO

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.secuso.privacyfriendlycodescanner.qrscanner.ui.activities

import android.content.Context
import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.secuso.privacyfriendlycodescanner.qrscanner.R
import com.secuso.privacyfriendlycodescanner.qrscanner.helpers.PreferenceKeys
import com.secuso.privacyfriendlycodescanner.qrscanner.ui.adapter.EditableListAdapter
import com.secuso.privacyfriendlycodescanner.qrscanner.ui.viewmodel.SettingsViewModel


class SettingsActivity : AppCompatActivity(R.layout.activity_settings) {
    class MainPreferenceFragment : PreferenceFragmentCompat() {
        private lateinit var viewModel: SettingsViewModel
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            viewModel = ViewModelProvider(this)[SettingsViewModel::class.java];
            setPreferencesFromResource(R.xml.preferences, rootKey)
            bindPreferenceSummaryToValue(findPreference(PreferenceKeys.SEARCH_ENGINE)!!)
            findPreference<Preference>(PreferenceKeys.APP_THEME)!!.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any ->
                    if (newValue == "DARK") {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    } else if (newValue == "LIGHT") {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    }
                    true
                }
            findPreference<Preference>("pref_url_classification_visited_urls")?.onPreferenceClickListener =
                Preference.OnPreferenceClickListener { preference: Preference? ->
                    createEditableListViewDialog(
                        R.string.url_dialog_settings_edit_visited_urls_title,
                        viewModel.urlEntities,
                        { EditableListAdapter.Data(getString(R.string.url_dialog_settings_visited_url_info, it.url, it.visits), it.id) },
                        { id -> viewModel.deleteURLEntity(id) },
                        requireContext()
                    ).show()
                    true
                }
            findPreference<Preference>("pref_url_classification_trusted_domains")?.onPreferenceClickListener =
                Preference.OnPreferenceClickListener { preference: Preference? ->
                    createEditableListViewDialog(
                        R.string.url_dialog_settings_edit_trusted_domains_title,
                        viewModel.domainEntities,
                        { EditableListAdapter.Data(it.baseDomain, it.id) },
                        { id -> viewModel.deleteDomainEntity(id) },
                        requireContext()
                    ).show()
                    true
                }
            findPreference<EditTextPreference>(PreferenceKeys.URL_CLASSIFICATION_GREY_CASE_WAITING_TIME)?.setOnBindEditTextListener { setEditTextProperties(it) }
        }

        private fun setEditTextProperties(editText: EditText) {
            editText.inputType = EditorInfo.TYPE_CLASS_NUMBER
            editText.setSingleLine()
            editText.setSelectAllOnFocus(true)
            editText.selectAll()
            editText.filters = arrayOf(InputFilter.LengthFilter(2))
        }

        private fun <T> createEditableListViewDialog(
            @StringRes title: Int,
            dataSource: LiveData<List<T>>,
            converter: (T) -> EditableListAdapter.Data,
            action: (id: Int) -> Unit,
            context: Context
        ): MaterialAlertDialogBuilder {
            val simpleAdapter = EditableListAdapter(action)

            val view = LayoutInflater.from(context).inflate(R.layout.dialog_editable_list, null, false)
            view.findViewById<RecyclerView>(R.id.item_list).apply {
                layoutManager = LinearLayoutManager(context)
                val dividerItemDecoration = DividerItemDecoration(
                    getContext(),
                    (layoutManager as LinearLayoutManager).orientation
                )
                addItemDecoration(dividerItemDecoration)
                adapter = simpleAdapter
            }
            dataSource.observe(this, { list -> simpleAdapter.updateData(list.map(converter)) })
            val builder: MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(context)
                .setView(view)
                .setTitle(title)
                .setCancelable(false)
                .setNegativeButton(R.string.close) { _, _ -> dataSource.removeObservers(this) }
            return builder
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        /**
         * A preference value change listener that updates the preference's summary
         * to reflect its new value.
         */
        private val sBindPreferenceSummaryToValueListener = Preference.OnPreferenceChangeListener { preference: Preference, value: Any ->
            val stringValue = value.toString()
            if (preference is ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                val listPreference = preference
                val index = listPreference.findIndexOfValue(stringValue)

                // Set the summary to reflect the new value.
                preference.setSummary(
                    if (index >= 0)
                        listPreference.entries[index]
                    else
                        null
                )
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.summary = stringValue
            }
            true
        }

        /**
         * Binds a preference's summary to its value. More specifically, when the
         * preference's value is changed, its summary (line of text below the
         * preference title) is updated to reflect the value. The summary is also
         * immediately updated upon calling this method. The exact display format is
         * dependent on the type of preference.
         *
         * @see .sBindPreferenceSummaryToValueListener
         */
        fun bindPreferenceSummaryToValue(preference: Preference) {
            // Set the listener to watch for value changes.
            preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener

            // Trigger the listener immediately with the preference's
            // current value.
            sBindPreferenceSummaryToValueListener.onPreferenceChange(
                preference,
                PreferenceManager
                    .getDefaultSharedPreferences(preference.context)
                    .getString(preference.key, "")
            )
        }
    }
}







