package mr.shtein.kennel.presentation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialSharedAxis
import mr.shtein.data.model.Animal
import mr.shtein.data.repository.UserPropertiesRepository
import mr.shtein.kennel.R
import mr.shtein.kennel.databinding.AnimalSettingsFragmentBinding
import mr.shtein.kennel.navigation.KennelNavigation
import mr.shtein.kennel.presentation.state.delete_animal.DeleteAnimalState
import mr.shtein.kennel.presentation.viewmodel.AnimalSettingsViewModel
import mr.shtein.network.ImageLoader
import mr.shtein.ui_util.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val ANIMAL_KEY = "animal_key"
private const val FROM_ADD_ANIMAL_REQUEST_KEY = "from_add_animal_request_key"
private const val RESULT_LISTENER_BUNDLE_KEY = "message_from_animal_card"
private const val DELETE_ANIMAL_MSG = "Питомец успешно удален"
private const val BUNDLE_KEY_FOR_ANIMAL_OBJECT = "animal_key"
private const val RESULT_LISTENER_KEY = "result_key"
private const val BINDING_IS_NULL = "AnimalSettingsFragmentBinding = null"

class AnimalSettingsFragment : Fragment(),
    OnSnapPositionChangeListener {
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var adapter: AnimalPhotoAdapter
    private var animal: Animal? = null
    private val userPropertiesRepository: UserPropertiesRepository by inject()
    private val networkImageLoader: ImageLoader by inject()
    private val navigator: KennelNavigation by inject()
    private val viewModel by viewModel<AnimalSettingsViewModel>()

    private var _binding: AnimalSettingsFragmentBinding? = null
    private val binding: AnimalSettingsFragmentBinding
        get() = _binding ?: throw RuntimeException(BINDING_IS_NULL)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        animal = arguments?.getParcelable(ANIMAL_KEY)

        setFragmentResultListener(FROM_ADD_ANIMAL_REQUEST_KEY) { _, bundle ->
            animal = bundle.getParcelable(BUNDLE_KEY_FOR_ANIMAL_OBJECT)
        }

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AnimalSettingsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        setInsetsListenerForPadding(
            view = view,
            left = false,
            top = false,
            right = false,
            bottom = true
        )
        setStatusBarColor(false)

        setDataToViews()
        setListeners()
    }

    override fun onResume() {
        super.onResume()
        with(binding) {
            animal?.let { animal ->
                animalSettingsName.text = animal.name
                animalSettingsBreed.text = animal.breed
                animalSettingsAge.text = animal.getAge()
                animalSettingsColor.text = animal.characteristics["color"]
                animalSettingsGender.text = animal.gender
                animalSettingsDescription.text = animal.description
                adapter.animalPhotos = animal.getImgUrls()
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun setDataToViews() {
        val photoRecyclerView = binding.animalSettingsPhotoContainer
        photoRecyclerView.setHasFixedSize(true)
        photoRecyclerView.layoutManager = layoutManager
        val snapHelper = PagerSnapHelper()
        val snapOnScrollListener =
            SnapOnScrollListener(snapHelper, this@AnimalSettingsFragment)
        photoRecyclerView.addOnScrollListener(snapOnScrollListener)
        snapHelper.attachToRecyclerView(photoRecyclerView)
        val animalPhotoInString = animal?.imgUrl?.map {
            it.url
        }
        adapter = AnimalPhotoAdapter(animalPhotoInString ?: listOf(), networkImageLoader)
        photoRecyclerView.adapter = adapter

        binding.animalSettingsPhotoCounter.text =
            getString(
                R.string.big_card_animal_photo_counter,
                1,
                animal?.imgUrl?.size ?: 1
            )
        with(binding) {
            animal?.let { animal ->
                animalSettingsName.text = animal.name
                animalSettingsBreed.text = animal.breed
                animalSettingsAge.text = animal.getAge()
                animalSettingsColor.text = animal.characteristics["color"]
                animalSettingsGender.text = animal.gender
                animalSettingsDescription.text = animal.description
            }
        }
    }

    private fun setListeners() {
        binding.animalSettingsDeleteBtn.setOnClickListener {
            buildAndShowDeleteAnimalDialog()
            Log.d("dialog", "Dialog was build")
        }

        binding.animalSettingsChangeBtn.setOnClickListener {
            animal?.let {
                navigator.moveToAddAnimal(animal = it, isFromSettings = true)
            }
        }
    }

    private fun buildAndShowDeleteAnimalDialog() {
        val token = userPropertiesRepository.getUserToken()

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.MyDialog)
            .setView(R.layout.animal_delete_dialog)
            //.setBackground(ColorDrawable(requireContext().getColor(R.color.transparent)))
            .show()

        val positiveBtn: Button? =
            dialog?.findViewById(R.id.animal_delete_dialog_positive_btn)
        val negativeBtn: Button? = dialog?.findViewById(R.id.animal_delete_dialog_negative_btn)
        val okBtn: Button? = dialog?.findViewById(R.id.animal_delete_dialog_ok_btn)
        val spinner: Spinner? = dialog?.findViewById(R.id.animal_delete_dialog_spinner)
        val motionLayout: MotionLayout? =
            dialog?.findViewById(R.id.animal_delete_dialog_motion_layout)

        positiveBtn?.setOnClickListener {

            viewModel.deleteAnimal(animal, requireContext())

            val bundle = bundleOf(
                RESULT_LISTENER_BUNDLE_KEY to DELETE_ANIMAL_MSG,
                ANIMAL_KEY to animal!!.id
            )

            setFragmentResult(RESULT_LISTENER_KEY, bundle)
            dialog.dismiss()
            navigator.backToPreviousFragment()

            viewModel.deleteState.observe(viewLifecycleOwner) {
                spinner?.isVisible = false
                when (it) {
                    is DeleteAnimalState.Loading -> spinner?.isVisible = true
                    is DeleteAnimalState.ErrorInfo -> {
                        showError(it.error)
                        if (motionLayout is MotionLayout) motionLayout.transitionToEnd()
                    }
                }
            }
            dialog.dismiss()
        }

        negativeBtn?.setOnClickListener {
            dialog.cancel()
        }

        okBtn?.setOnClickListener {
            dialog.cancel()
        }
    }
    private fun showError(errorText: String) {
        Toast.makeText(requireContext(), errorText, Toast.LENGTH_LONG).show()
    }

    override fun onSnapPositionChange(position: Int) {
        val elementsCount = animal?.imgUrl?.size
        binding.animalSettingsPhotoCounter.text =
            getString(
                R.string.big_card_animal_photo_counter,
                position + 1,
                elementsCount
            )
    }
}
