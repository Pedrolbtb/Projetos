package com.companyvihva.vihva.Inicio

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.telephony.SmsManager
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.companyvihva.vihva.Configurações.Configuracoes
import com.companyvihva.vihva.R
import com.companyvihva.vihva.model.Adapter.AdapterListanova
import com.companyvihva.vihva.model.Adapter.AdapterRemedio
import com.companyvihva.vihva.model.Tipo_Classe
import com.companyvihva.vihva.model.Tipo_Remedios
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.coroutines.NonDisposableHandle.parent

class Inicio1 : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var adapterListanova: AdapterListanova
    private lateinit var listaInicio: MutableList<Tipo_Remedios>
    private lateinit var urlImageView: ImageView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var btnSOS: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_inicio1, container, false)
        val textAviso: TextView? = view.findViewById(R.id.textViewAviso)
        val fullText = "Texto Exemplo"
        val spannableString = SpannableString(fullText)

        val redColor = ContextCompat.getColor(requireContext(), R.color.vermelho_alerta)
        spannableString.setSpan(
            ForegroundColorSpan(redColor),
            0,
            1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        textAviso?.text = spannableString

        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        btnSOS = view.findViewById(R.id.sos)
        btnSOS.setOnClickListener {
            requestPermissions(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.SEND_SMS
            )
        }

        setupRecyclerView(view)
        fetchRemediosDoUsuario()
        setupDiabetesInfo(view)

        // Adicionando o listener para o botão de imagem de lixeira global
        val imageLixeiraGlobal: ImageView = view.findViewById(R.id.image_lixeira_global)
        imageLixeiraGlobal.setOnClickListener {
            deletarArrayRemedios()
        }

        return view
    }

    // Método setupRecyclerView
    private fun setupRecyclerView(view: View) {
        val recyclerview_remedioAdicionado =
            view.findViewById<RecyclerView>(R.id.Recyclerview_remedioAdicionado)
        recyclerview_remedioAdicionado.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        recyclerview_remedioAdicionado.setHasFixedSize(true)
        listaInicio = mutableListOf()
        adapterListanova = AdapterListanova(requireContext(), listaInicio)
        recyclerview_remedioAdicionado.adapter = adapterListanova
    }

    // Método fetchRemediosDoUsuario
    private fun fetchRemediosDoUsuario() {
        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid

        if (uid != null) {
            val clientRef = db.collection("clientes").document(uid)
            clientRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val remediosIds = document.get("remedios") as? List<String>
                        remediosIds?.let {
                            for (remedioId in it) {
                                fetchDadosDoFirebase(remedioId)
                            }
                        }
                    } else {
                        Log.d("Inicio1", "Documento do cliente não encontrado")
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("Inicio1", "Erro ao obter documento do cliente", e)
                }
        }
    }

    // Método fetchDadosDoFirebase
    private fun fetchDadosDoFirebase(remedioId: String) {
        val docRef = db.collection("remedios").document(remedioId)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nome = document.getString("nome")
                    val tipo = document.getString("tipo")
                    val descricao = document.getString("descricao")
                    val url = document.getString("Url")

                    if (nome != null && tipo != null && descricao != null) {
                        val remedio = Tipo_Remedios(url ?: "", nome, tipo, descricao)
                        atualizarListaRemedios(remedio)
                    } else {
                        Log.d("Inicio1", "Nome, tipo ou descricao do remédio está nulo")
                    }
                } else {
                    Log.d("Inicio1", "Documento do remédio não encontrado")
                }
            }
            .addOnFailureListener { e ->
                Log.w("Inicio1", "Erro ao obter documento do remédio", e)
            }
    }

    // Método atualizarListaRemedios
    private fun atualizarListaRemedios(remedio: Tipo_Remedios) {
        listaInicio.add(remedio)
        adapterListanova.notifyDataSetChanged()
    }

    // Método setupDiabetesInfo
    private fun setupDiabetesInfo(view: View) {
        val doencaRef = db.collection("doenca").document("diabetes")
        doencaRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nome = document.getString("nome") ?: ""
                    val imageUrl = document.getString("Url") ?: ""
                    val nomeTextView: TextView = view.findViewById(R.id.nome_widget)
                    val imageView1: ImageView = view.findViewById(R.id.image_widget)
                    nomeTextView.text = nome
                    if (imageUrl.isNotEmpty()) {
                        Picasso.get().load(imageUrl).into(imageView1)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "URL da imagem não encontrado",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Documento não encontrado", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    requireContext(),
                    "Erro ao carregar dados: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // Método para deletar o array de remédios do usuário autenticado
    private fun deletarArrayRemedios() {
        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid

        if (uid != null) {
            val clientRef = db.collection("clientes").document(uid)
            clientRef.update("remedios", FieldValue.delete())
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Lista de remédios deletado com sucesso", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Erro ao deletar o lista de remédios: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), "Erro: UID do usuário não encontrado", Toast.LENGTH_SHORT).show()
        }
    }

    // Método requestPermissions
    private fun requestPermissions(vararg permissions: String) {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                permissions[0]
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                requireContext(),
                permissions[1]
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                requireContext(),
                permissions[2]
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(requireActivity(), permissions, 0)
        } else {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->

                if (location != null) {
                    val preferences = requireContext().getSharedPreferences(
                        Configuracoes.PREF_NAME, AppCompatActivity.MODE_PRIVATE)

                    val savedMessage = preferences.getString(
                        Configuracoes.KEY_DEFAULT_MSG,
                        "Mensagem padrão não definida"
                    )

                    val smsMessage = "$savedMessage\nhttps://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"

                    val smsManager: SmsManager = SmsManager.getDefault()
                    smsManager.sendTextMessage(
                        "+5519989769783",
                        null,
                        smsMessage,
                        null,
                        null
                    )

                    Toast.makeText(
                        requireContext(),
                        "SMS enviado com a localização e a mensagem salva!",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Localização não disponível",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Erro ao obter localização: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
