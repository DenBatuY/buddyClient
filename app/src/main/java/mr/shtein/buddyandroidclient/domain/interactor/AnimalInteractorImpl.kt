package mr.shtein.buddyandroidclient.domain.interactor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mr.shtein.buddyandroidclient.data.repository.*
import mr.shtein.buddyandroidclient.exceptions.validate.IncorrectDataException
import mr.shtein.buddyandroidclient.exceptions.validate.ServerErrorException
import mr.shtein.buddyandroidclient.model.Animal
import mr.shtein.buddyandroidclient.model.Coordinates
import mr.shtein.buddyandroidclient.model.dto.*
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlin.jvm.Throws

private const val COLOR_ID = 1

interface AnimalInteractor {
    @Throws(ConnectException::class, SocketTimeoutException::class, ServerErrorException::class)
    suspend fun getAnimalsByFilter(animalFilter: AnimalFilter): List<Animal>
    suspend fun getDistancesFromUser(token: String, coordinates: Coordinates): HashMap<Int, Int>
    suspend fun getAnimalTypes(): List<FilterAutocompleteItem>
    suspend fun getAnimalBreeds(animalTypeList: List<Int>): List<FilterAutocompleteItem>
    suspend fun getAnimalCharacteristics(characteristicId: Int): List<FilterAutocompleteItem>
    suspend fun getAnimalsCountByFilter(animalFilter: AnimalFilter): Int
}

class AnimalInteractorImpl(
    private val animalRepository: AnimalRepository,
    private val animalBreedRepository: AnimalBreedRepository,
    private val sharedUserPropertiesRepository: UserPropertiesRepository,
    private val animalCharacteristicsRepository: AnimalCharacteristicsRepository,
    private val retrofitDistanceCounterRepository: DistanceCounterRepository,
    private val animalTypeRepository: AnimalTypeRepository
) : AnimalInteractor {
    override suspend fun getAnimalsByFilter(animalFilter: AnimalFilter): List<Animal> =
        withContext(Dispatchers.IO) {
            return@withContext animalRepository.getAnimals(animalFilter)
        }

    override suspend fun getDistancesFromUser(
        token: String,
        coordinates: Coordinates
    ): HashMap<Int, Int> {
        return retrofitDistanceCounterRepository.getDistancesFromUser(token, coordinates)
    }

    override suspend fun getAnimalTypes(): List<FilterAutocompleteItem> {
        val animalType = animalTypeRepository.getAnimalTypes()
        return mapAnimalTypeToFilterItem(animalType)
    }

    override suspend fun getAnimalBreeds(animalTypeList: List<Int>): List<FilterAutocompleteItem> {
        val token = getUserToken()
        val allBreeds = mutableListOf<Breed>()
        animalTypeList.forEach {
            val animalBreeds =
                animalBreedRepository.getAnimalBreeds(token = token, animalTypeId = it)
            allBreeds.addAll(animalBreeds)
        }
        return mapBreedsToFilterBreeds(breeds = allBreeds)
    }

    override suspend fun getAnimalCharacteristics(characteristicId: Int): List<FilterAutocompleteItem> {
        val token = getUserToken()
        return when (characteristicId) {
            COLOR_ID -> {
                val animalColors = animalCharacteristicsRepository.getAnimalColors(token)
                mapCharacteristicToFilterItem(animalColors)
            }
            else -> {
                throw IncorrectDataException()
            }
        }
    }

    override suspend fun getAnimalsCountByFilter(animalFilter: AnimalFilter): Int =
        withContext(Dispatchers.IO) {
            return@withContext animalRepository.getAnimalsCountByFilter(animalFilter)
        }

    private fun mapAnimalTypeToFilterItem(
        animalType: List<AnimalType>
    ): List<FilterAutocompleteItem> {
        return animalType
            .map {
                FilterAutocompleteItem(id = it.id, name = it.pluralAnimalType, isSelected = false)
            }
            .toList()
    }

    private fun mapCharacteristicToFilterItem(
        animalColors: List<AnimalCharacteristic>
    ): List<FilterAutocompleteItem> {
        return animalColors
            .map {
                FilterAutocompleteItem(id = it.id, name = it.name, isSelected = false)
            }
            .toList()
    }

    private fun getUserToken(): String {
        return sharedUserPropertiesRepository.getUserToken()
    }

    private fun mapBreedsToFilterBreeds(breeds: List<Breed>): List<FilterAutocompleteItem> {
        return breeds
            .map {
                FilterAutocompleteItem(id = it.id, name = it.name, isSelected = false)
            }
            .toList()
    }

}