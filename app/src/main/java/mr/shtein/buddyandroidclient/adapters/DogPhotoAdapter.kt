package mr.shtein.buddyandroidclient.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import mr.shtein.buddyandroidclient.R
import mr.shtein.buddyandroidclient.model.Animal

class DogPhotoAdapter(
    private val animalsList: List<Animal>,
    val token: String,
    val animalTouchCallback: OnAnimalItemClickListener
): RecyclerView.Adapter<DogPhotoAdapter.AnimalInKennelViewHolder>() {

    private lateinit var host: String
    private val pathForAnimalPhoto = "animal/photo/"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimalInKennelViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        host = parent.context.resources.getString(R.string.host)
        return AnimalInKennelViewHolder(
            inflater.inflate(R.layout.animal_in_kennel_row, parent, false),
            animalTouchCallback
        )
    }

    override fun onBindViewHolder(holder: AnimalInKennelViewHolder, position: Int) {
        val animalCard = getItem(position)
        holder.bind(animalCard)
    }

    override fun getItemCount(): Int {
        return animalsList.size
    }

    private fun getItem(position: Int): Animal = animalsList[position]



    inner class AnimalInKennelViewHolder(
        private val itemView: View,
        private val onItemListener: OnAnimalItemClickListener
    ): RecyclerView.ViewHolder(itemView), View.OnClickListener {

        private val avatar = itemView.findViewById<ImageButton>(R.id.animal_in_kennel_avatar)

        fun bind(animalCard: Animal) {
            val animalAvatarUrl = animalCard.imgUrl.find {
                it.primary
            }
            val fullUrl = "$host/$pathForAnimalPhoto${animalAvatarUrl?.url}"
            val header = LazyHeaders.Builder()
                .addHeader("Authorization", "Bearer $token")
                .build()
            val urlWithHeaders = GlideUrl(
                fullUrl,
                header
            )
            Glide.with(itemView.context)
                .load(urlWithHeaders)
                .into(avatar)
        }


        override fun onClick(p0: View?) {
            onItemListener.onClick(animalsList[absoluteAdapterPosition])
        }
    }

    interface OnAnimalItemClickListener {
        fun onClick(animalItem: Animal)
    }
}