package com.art3mis.trivers

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.art3mis.trivers.adaptador.LoadingViewHolder
import com.art3mis.trivers.adaptador.nullViewHolder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_match.*
import kotlinx.android.synthetic.main.item_usuario.view.*
import java.util.*

internal class ItemViewHolderUser(itemView: View) : RecyclerView.ViewHolder(itemView){
    var name: TextView = itemView.textView_Name
    var photo: ImageView = itemView.imageView
}

internal class AdaptadorUsuario(reciclerView: RecyclerView, private var activity: Activity, private var itemUsuario: MutableList<Item_Usuario>): RecyclerView.Adapter<RecyclerView.ViewHolder>(){
    private val VIEW_TYPE_ITEM=0
    private val VIEW_TYPE_LOADING=1

    override fun getItemCount(): Int {
        return itemUsuario.size
    }

    override fun getItemViewType(position: Int): Int {
        return if(itemUsuario[position]==null)VIEW_TYPE_LOADING else VIEW_TYPE_ITEM
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(holder is ItemViewHolderUser){
            itemUsuario[position]
            holder.name.text = itemUsuario[position].name
        }
        else if(holder is LoadingViewHolder){
            holder.progressBar.isIndeterminate = true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if(viewType==VIEW_TYPE_ITEM){
            val view = LayoutInflater.from(activity).inflate(R.layout.item_usuario,parent,false)
            return ItemViewHolderUser(view)
        }
        else if(viewType==VIEW_TYPE_LOADING){
            val view = LayoutInflater.from(activity).inflate(R.layout.item_cargando,parent,false)
            return LoadingViewHolder(view)
        }
        return  nullViewHolder(view = LayoutInflater.from(activity).inflate(R.layout.item_cargando,parent,false))
    }

}

data class Item_Usuario (var name: String){
    lateinit var completeName: String
}

class MatchActivity : AppCompatActivity() {
    private lateinit var dbRefgetTrivias: DatabaseReference
    private lateinit var dbRefgetUsers: DatabaseReference
    private lateinit var spinner: Spinner
    private lateinit var adapterTematica: ArrayAdapter<CharSequence>
    private lateinit var recyclerView: RecyclerView
    private val activity=this
    private lateinit var adapter: AdaptadorUsuario
    private var itemUsuario: MutableList<Item_Usuario> = ArrayList()
    private lateinit var Ref: String
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_match)
        navigation()

        spinner = findViewById(R.id.spinner)
        adapterTematica= ArrayAdapter<CharSequence>(this,R.layout.simple_spiner_tematicas)
        adapterTematica.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        recyclerView = findViewById(R.id.recicler_view_match)
        cargarTematicas()

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Ref = parent!!.getItemAtPosition(position).toString()
                getRTrivias()
                itemUsuario.clear()
            }
        }
    }

    private fun getRTrivias(){
        recyclerView.layoutManager =LinearLayoutManager(this)

        dbRefgetTrivias = FirebaseDatabase.getInstance().getReference("Users/" + FirebaseAuth.getInstance().uid.toString() + "/Trivias/$Ref")

        dbRefgetTrivias.addListenerForSingleValueEvent(object :ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(p0: DataSnapshot) {
               for (j in p0.children){
                   var i=0
                   var valor=0.0
                   for(dS in j.children){
                       valor += (dS.value).toString().toInt()
                       i++
                   }
                   valor /= i

                   val vM = when (valor){
                       in 0..20 -> 20.toString()
                       in 21..50 ->50.toString()
                       in 51..70 ->70.toString()
                       else -> 100.toString()
                   }

                   dbRefgetUsers = FirebaseDatabase.getInstance().getReference("Matching/$Ref/${j.key}/$vM")

                   dbRefgetUsers.addValueEventListener(object :ValueEventListener{
                       override fun onCancelled(p0: DatabaseError) {}
                       override fun onDataChange(dS: DataSnapshot) {

                           for(k in dS.children){
                               if (k.key.toString() != FirebaseAuth.getInstance().uid){
                                   val userUId: String = k.key.toString()

                                   databaseReference = FirebaseDatabase.getInstance().getReference("Users/$userUId")
                                   databaseReference.addValueEventListener(object :ValueEventListener{
                                       override fun onCancelled(p0: DatabaseError) {
                                           TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                                       }

                                       override fun onDataChange(p0: DataSnapshot) {
                                           val item = Item_Usuario(if (p0.child("lastName").value.toString() != "NoLastName"){
                                               p0.child("name").value.toString() + " " + p0.child("lastName").value.toString()
                                           } else{
                                               p0.child("name").value.toString()
                                           })
                                           itemUsuario.add(item)
                                           //Inicializando Vista
                                           adapter = AdaptadorUsuario(recyclerView, activity , itemUsuario)
                                           recyclerView.adapter = adapter
                                       }

                                   })
                               }
                           }

                       }
                   })
               }
            }
        })
    }

    private fun cargarTematicas() {
        dbRefgetTrivias = FirebaseDatabase.getInstance().getReference("Trivias/Tematicas")
        dbRefgetTrivias.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {}
            override fun onDataChange(dS: DataSnapshot) {
//                adapterTematica.add("Tod")
                for (i in dS.children) {
                    adapterTematica.add(i.key.toString())
                }
                adapterTematica.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapterTematica
            }
        })
    }

    fun navigation(){
        navigation.selectedItemId = R.id.navigation_match
        navigation.setOnNavigationItemSelectedListener(BottomNavigationView.OnNavigationItemSelectedListener { item ->
            when {
                item.itemId == R.id.navigation_profile -> startActivity(Intent(this@MatchActivity, PrivateProfileActivity::class.java))
                item.itemId == R.id.navigation_match -> startActivity(Intent(this@MatchActivity, MatchActivity::class.java))
                item.itemId == R.id.navigation_trivias -> startActivity(Intent(this@MatchActivity, TriviasTemasActivity::class.java))
                else -> return@OnNavigationItemSelectedListener true
            }
            false
        })
    }
}
