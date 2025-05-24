import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.pokeapp.pokeApk.data.localDatabase.model.PokemonEntity
import com.example.pokeapp.R

class PokemonAdapter : RecyclerView.Adapter<PokemonAdapter.ViewHolder>() {

    private val pokemons = mutableListOf<PokemonEntity>()

    fun addPokemon(pokemon: PokemonEntity) {
        if (pokemons.any { it.id == pokemon.id }) return
        pokemons.add(pokemon)
        notifyItemInserted(pokemons.size - 1)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val id : TextView = view.findViewById(R.id.txtPokemonId)
        val name: TextView = view.findViewById(R.id.txtPokemonName)
        val image: ImageView = view.findViewById(R.id.imgPokemon)
        val btnSeeMore : ImageView = view.findViewById(R.id.btnSeeMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_item_pokemon, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = pokemons.size
    internal var onSeeMoreClick: ((PokemonEntity) -> Unit)? = null
    internal var onAgregarClick: ((PokemonEntity) -> Unit)? = null


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pokemon = pokemons[position]
        holder.name.text = pokemon.name.replaceFirstChar { it.uppercaseChar() }
        holder.id.text = "#${pokemon.id.toString().padStart(3, '0')}"
        Glide.with(holder.image.context)
            .load(pokemon.spriteUrl)
            .into(holder.image)

        holder.btnSeeMore.setOnClickListener {
            onSeeMoreClick?.invoke(pokemon)
        }
    }

}
