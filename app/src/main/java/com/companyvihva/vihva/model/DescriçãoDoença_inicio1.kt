package com.companyvihva.vihva.com.companyvihva.vihva.model

import MedicoAdapter
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.companyvihva.vihva.R
import com.companyvihva.vihva.model.Tipo_Classe
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DescriçãoDoença_inicio1 : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var urlImageView: ImageView
    private lateinit var medicoSpinner: Spinner
    private var listaMedicos: MutableList<medico_spinner> = mutableListOf()
    private var doenca: Tipo_Classe? = null
    private var doencaid: String? = null
    private var medicoMap: MutableMap<String, String> = mutableMapOf()
    private var formattedDate: String? = null
    private lateinit var nomeTextView: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.popup_desc_doenca_inicio1)

        // Inicializa o Firestore e o Auth
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Inicializa as Views
        medicoSpinner = findViewById(R.id.medicoSpinner)
        urlImageView = findViewById(R.id.foto_Doenca)
        val textViewCalendario = findViewById<TextView>(R.id.textView_receitado)

        nomeTextView = findViewById(R.id.nomeDoenca)

        // Obtém o ID da doença passada pelo Intent
        doencaid = intent.getStringExtra("doencaId")

        // Configura o botão de voltar
        val btnVoltar = findViewById<ImageButton>(R.id.btn_voltarDO)
        btnVoltar.setOnClickListener {
            onBackPressed()
        }

        // Configura o botão de excluir
        val btnExcluir = findViewById<ImageButton>(R.id.lixeira_doencas)
        btnExcluir.setOnClickListener {
            doencaid?.let { id ->
                showConfirmDeleteDialog(id)
            }
        }

        // Carrega a lista de médicos do Firebase
        carregarMedicos()

        // Busca os dados da doença no Firebase
        doencaid?.let { id ->
            fetchDadosDoFirebase(id)
        }

        // Configura o DatePicker para selecionar a data
        textViewCalendario.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Selecione quando este remédio foi prescrito")
                .build()
            datePicker.addOnPositiveButtonClickListener { selectedDate ->
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                formattedDate = sdf.format(Date(selectedDate))
                textViewCalendario.text = formattedDate
            }
            datePicker.show(supportFragmentManager, "DatePicker")
        }
    }

    private fun fetchDadosDoFirebase(docId: String) {
        val docRef = firestore.collection("doenca").document(docId)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val nome = document.getString("nome")
                    val descricao = document.getString("descricao")
                    val url = document.getString("Url")

                    // Carrega a Url de imagem usando o Picasso
                    url?.let {
                        Picasso.get().load(it).into(urlImageView)
                    }
                    nomeTextView.text = nome

                    // Cria um objeto Tipo_Classe com os dados obtidos no Firestore
                    doenca = Tipo_Classe(url ?: "", nome ?: "", descricao ?: "")

                    // Atualiza as TextViews com os dados de nome e descrição
                    // Adicione aqui o código para atualizar as TextViews (caso existam)
                } else {
                    Log.d("PopupDoenca", "Documento não encontrado")
                }
            }
            .addOnFailureListener { e ->
                Log.w("PopupDoenca", "Erro ao obter documento", e)
            }
    }

    // Método para o Alert Dialog da doença
    private fun showConfirmDeleteDialog(doencaId: String) {
        AlertDialog.Builder(this).apply {
            setTitle("Confirmação de Exclusão")
            setMessage("Tem certeza que deseja excluir esta doença? Você pode adicioná-la novamente na lista de doenças")
            setPositiveButton("Sim") { _, _ ->
                deleteDoencaArray(doencaId)
            }
            setNegativeButton("Não", null)
            create()
            show()
        }
    }

    // Método para excluir a doença individualmente
    private fun deleteDoencaArray(doencaId: String) {
        val user = auth.currentUser
        user?.let {
            val userDocRef = firestore.collection("clientes").document(it.uid)
            userDocRef.update("doenca", FieldValue.arrayRemove(doencaId))
                .addOnSuccessListener {
                    Toast.makeText(this, "Doença excluída do seu perfil com sucesso", Toast.LENGTH_SHORT).show()
                    Log.d("DescriçãoDoença_Inicio1", "Sucesso ao remover doença do Array")
                    onBackPressed()
                }
                .addOnFailureListener { e ->
                    Log.w("DescriçãoDoença_Inicio1", "Erro ao excluir doença do array", e)
                }
        }
    }

    private fun carregarMedicos() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("clientes")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val medicosArray = document.get("medicos") as? List<String>
                if (medicosArray != null) {
                    val medicosList = mutableListOf<medico_spinner>()
                    medicosArray.forEach { medicoUid ->
                        firestore.collection("medicos").document(medicoUid).get().addOnSuccessListener { medicoDoc ->
                            val nomeMedico = medicoDoc.getString("nome") ?: medicoUid
                            val imageUrl = medicoDoc.getString("imageUrl") ?: ""
                            val medico = medico_spinner(nomeMedico, imageUrl)
                            medicosList.add(medico)
                            medicoMap[medicoUid] = nomeMedico

                            // Atualiza o adapter do Spinner
                            val adapter = MedicoAdapter(this, medicosList)
                            medicoSpinner.adapter = adapter
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Evento", "Erro ao carregar médicos", e)
            }
    }
}
