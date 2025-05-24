import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pokeapp.R
import com.pokeapp.pokeApk.data.localDatabase.model.PokemonEntity

class TeamAdapter(
    internal var onItemClick: (PokemonEntity) -> Unit
) : ListAdapter<PokemonEntity, TeamAdapter.TeamViewHolder>(DiffCallback) {

    inner class TeamViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgPokemon: ImageView = view.findViewById(R.id.imgPokemon)
        val txtName: TextView = view.findViewById(R.id.txtPokemonName)
        val txtLevel: TextView = view.findViewById(R.id.txtPokemonLevel)
        val progressBar: ProgressBar = view.findViewById(R.id.pbNivel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_team_pokemon, parent, false)
        return TeamViewHolder(view)
    }

    override fun onBindViewHolder(holder: TeamViewHolder, position: Int) {
        val pokemon = getItem(position)

        holder.txtName.text = pokemon.name.replaceFirstChar { it.uppercaseChar() }
        Glide.with(holder.imgPokemon.context)
            .load(pokemon.spriteUrl)
            .into(holder.imgPokemon)

        holder.txtLevel.text = "Nivel ${pokemon.nivel}"
        holder.progressBar.progress = pokemon.exp
        holder.progressBar.max = 100

        holder.itemView.setOnClickListener {
            onItemClick(pokemon)
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<PokemonEntity>() {
        override fun areItemsTheSame(oldItem: PokemonEntity, newItem: PokemonEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PokemonEntity, newItem: PokemonEntity): Boolean {
            return oldItem == newItem
        }
    }
}
