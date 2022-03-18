package com.defendend.criminalintent

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import java.io.File
import java.lang.Exception
import java.util.*

private const val ARG_CRIME_ID = "crime_id"
private const val DIALOG_DATE = "DialogDate"
private const val DIALOG_TIME = "DialogTime"
private const val DIALOG_PICTURE = "DialogPicture"
private const val REQUEST_DATE = 0
private const val REQUEST_TIME = 1
private const val REQUEST_CONTACT = 2
private const val REQUEST_PHONE = 3
private const val REQUEST_PHOTO = 4
private const val REQUEST_DIALOG_PHOTO = 5
private const val DATE_FORMAT = "EEE, MMM, dd"

class CrimeFragment : Fragment(), DatePickerFragment.Callbacks, TimePickerFragment.Callbacks {
    private var crime: Crime = Crime()
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private lateinit var titleFieldEditText: EditText
    private lateinit var dateButton: Button
    private lateinit var timeButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var reportButton: Button
    private lateinit var suspectButton: Button
    private lateinit var callSuspect: Button
    private lateinit var photoButton: ImageButton
    private lateinit var photoView: ImageView
    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProviders.of(this).get(CrimeDetailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val crimeId: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        crimeDetailViewModel.loadCrime(crimeId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime, container, false)

        view.apply {
            titleFieldEditText = findViewById(R.id.crimeTitle)
            dateButton = findViewById(R.id.crimeDate)
            timeButton = findViewById(R.id.crimeTimeButton)
            solvedCheckBox = findViewById(R.id.crime_solved)
            reportButton = findViewById(R.id.crimeReport)
            suspectButton = findViewById(R.id.crimeSuspect)
            callSuspect = findViewById(R.id.crimeCallSuspect)
            photoButton = findViewById(R.id.crimeImageButton)
            photoView = findViewById(R.id.crimeImageView)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(viewLifecycleOwner) { crime ->
            crime?.let {
                this.crime = crime
                photoFile = crimeDetailViewModel.getPhotoFile(crime)
                photoUri = FileProvider.getUriForFile(
                    requireActivity(),
                    "com.defendend.criminalintent.fileprovider",
                    photoFile
                )
                updateUI()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(
                sequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun afterTextChanged(sequence: Editable?) {

            }

            override fun onTextChanged(
                sequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                crime.title = sequence.toString()
            }
        }

        titleFieldEditText.addTextChangedListener(titleWatcher)

        solvedCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                crime.isSolved = isChecked
            }
        }

        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(crime.date).apply {
                setTargetFragment(this@CrimeFragment, REQUEST_DATE)
                show(this@CrimeFragment.parentFragmentManager, DIALOG_DATE)
            }
        }

        timeButton.setOnClickListener {
            TimePickerFragment.newInstance(crime.date).apply {
                setTargetFragment(this@CrimeFragment, REQUEST_TIME)
                show(this@CrimeFragment.parentFragmentManager, DIALOG_TIME)
            }
        }

        photoView.setOnClickListener {
            if (photoFile.exists()) {
                PhotoLargeFragment.newInstance(photoFile.path).apply {
                    show(this@CrimeFragment.parentFragmentManager, DIALOG_PICTURE)
                }
            }
        }

        reportButton.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    getString(R.string.crime_report_subject)
                )
            }.also {
                val chooserIntent =
                    Intent.createChooser(it, getString(R.string.send_report))
                startActivity(chooserIntent)
            }
        }

        suspectButton.apply {
            val pickContactIntent =
                Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)

            setOnClickListener {
                startActivityForResult(pickContactIntent, REQUEST_CONTACT)
            }


            val packageManager: PackageManager = requireActivity().packageManager
            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(pickContactIntent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolvedActivity == null)
                isEnabled = false
        }

        callSuspect.apply {

            val pickPhoneIntent =
                Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)

            setOnClickListener {
                if (crime.suspectPhone == "") {
                    startActivityForResult(pickPhoneIntent, REQUEST_PHONE)
                } else {
                    val dialNumber = Intent(Intent.ACTION_DIAL)
                    dialNumber.data = Uri.parse("tel:${crime.suspectPhone}")
                    startActivity(dialNumber)
                }
            }

            val packageManager: PackageManager = requireActivity().packageManager
            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(pickPhoneIntent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolvedActivity == null)
                isEnabled = false

        }

        photoButton.apply {
            val packageManager: PackageManager = requireActivity().packageManager

            val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(
                    captureImage,
                    PackageManager.MATCH_DEFAULT_ONLY
                )

            if (resolvedActivity == null) {
                isEnabled = false
            }

            setOnClickListener {
                captureImage.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

                val cameraActivities: List<ResolveInfo> = packageManager.queryIntentActivities(captureImage,
                PackageManager.MATCH_DEFAULT_ONLY)

                for (cameraActivity in cameraActivities) {
                    requireActivity().grantUriPermission(
                        cameraActivity.activityInfo.packageName,
                        photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }

                startActivityForResult(captureImage, REQUEST_PHOTO)
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            resultCode != Activity.RESULT_OK -> return

            requestCode == REQUEST_CONTACT && data != null -> {

                val contactUri: Uri = data.data ?: return
                // указатель для каких полей наш запрос должен возвращать значения
                // Выполняемый здесь запрос = contactUri похож на предложение "where"
                val queryField = ContactsContract.CommonDataKinds.Phone._ID

                val cursor = contactUri.let {
                    requireActivity().contentResolver
                        .query(
                            it,
                            null,
                            queryField,
                            null,
                            null
                        )
                }
                cursor?.let {
                    if (it.count == 0) return
                    //первый столбец первой строки данных -
                    // это имя нашего подозреваемого
                    it.moveToFirst()
                    try {
                        val suspect =
                            it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                                ?: ""
                        crime.suspect = suspect
                        crimeDetailViewModel.saveCrime(crime)
                        suspectButton.text = suspect

                        val suspectPhone =
                            it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                                ?: ""
                        crime.suspectPhone = suspectPhone
                        crimeDetailViewModel.saveCrime(crime)
                        callSuspect.text = suspectPhone

                    } catch (ex: Exception) {

                    }
                }


            }

            requestCode == REQUEST_PHONE && data != null -> {

                val contactUri: Uri = data.data ?: return

                val queryFields = ContactsContract.CommonDataKinds.Phone._ID

                val cursor =
                    requireActivity().contentResolver
                        .query(contactUri, null, queryFields, null, null)

                cursor.use {
                    if (it?.count == 0) return

                    it?.moveToFirst()

                    try {
                        val number =
                            it?.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER))
                                ?: ""

                        val dialNumber = Intent(Intent.ACTION_DIAL)
                        dialNumber.data = Uri.parse("tel:$number")
                        startActivity(dialNumber)
                    } catch (ex: Exception) {

                    }

                }
            }

            requestCode == REQUEST_PHOTO -> {
                requireActivity().revokeUriPermission(photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                updatePhotoView()
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().revokeUriPermission(photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    private fun updateUI() {
        titleFieldEditText.setText(crime.title)
        dateButton.text = crime.date.toString()
        solvedCheckBox.apply {
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState()
        }
        if (crime.suspect.isNotEmpty()) {
            suspectButton.text = crime.suspect
        }
        updatePhotoView()
    }

    private fun getCrimeReport(): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }

        val dateString = DateFormat.format(DATE_FORMAT, crime.date).toString()

        val suspect = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }

        return getString(
            R.string.crime_report,
            crime.title, dateString, solvedString, suspect
        )
    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    override fun onDateSelected(date: Date) {
        crime.date = date
        updateUI()
    }

    override fun onTimeSelected(date: Date) {
        crime.date = date
        updateUI()
    }

    private fun updatePhotoView() {
        if (photoFile.exists()) {
            val bitmap = getScaledBitmap(photoFile.path, requireActivity())
            photoView.setImageBitmap(bitmap)
            photoView.contentDescription = getString(R.string.crime_photo_image_description)
        } else {
            photoView.setImageDrawable(null)
            photoView.contentDescription = getString(R.string.crime_photo_no_image_description)
        }
    }

    companion object {
        fun newInstance(crimeId: UUID): CrimeFragment {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }

            return CrimeFragment().apply {
                arguments = args
            }
        }
    }
}