function RilevDatiPrdTSOL() {
	parent.document.getElementById('Indietro').style.display = displayBlock;
	parent.document.getElementById('StampaBolla').style.display = displayNone;
	parent.document.getElementById('ConfermaStampa').style.display = displayNone;
	parent.document.getElementById('ProssimaAtvBut').style.display = displayNone;
	parent.document.getElementById('MonitorBut').style.display = displayNone;
	parent.document.getElementById('YChiamataUdcBut').style.display = displayNone;
	parent.document.getElementById('YRiposizionamentoUdcBut').style.display = displayNone;
	parent.document.getElementById('YChiudiListaTesta').style.display = displayNone; //71979
	parent.document.getElementById('YCleanLoftware').style.display = displayNone; //72147
	document.forms[0].CodiceTestataLista.addEventListener("keydown", function(event) {
		if (event.key === "Enter") {
			event.preventDefault(); // Stop form submission
		}
	});
	parent.document.getElementById('Conferma').style.display = displayNone; //AGSOF3 disabilito conferma finche non cliccano check
}

function conferma() {
	document.getElementById("spinner").style.display = "flex";//72435 mostro lo spinner
	var className = eval("document.forms[0].thClassName.value");
	runActionDirect('CONFERMA_CHIUSURA_LISTA_TESTA', 'action_submit', className, null, 'same', 'no');
}

function checkButtonAction(){
	document.getElementById("spinner").style.display = "flex";
	 const codLista = document.getElementById("CodiceTestataLista").value;

    fetch('servlet/it.dnd.thip.produzione.raccoltaDati.web.YCheckChiusuraLista?codLista=' + encodeURIComponent(codLista))
        .then(response => response.json())
        .then(data => {
            displayResult(data);
            parent.document.getElementById('Conferma').style.display = "block"; 
            document.getElementById("spinner").style.display = "none";
        })
        .catch(error => console.error('Error:', error));
}

function displayResult(data) {
    const container = document.getElementById("result");

    // Logica corretta
    const completed = data.filter(r => 
        r.qtaResidua == 0 && r.attivitaNonDichiarata !== 'Y'
    );

    const pending = data.filter(r => 
        r.qtaResidua != 0 || r.attivitaNonDichiarata === 'Y'
    );

    let html = "";

    html += createSection("Match completo", completed, true);
    html += createSection("Anomalie", pending, false);

    container.innerHTML = html;
}


// Create collapsible section
function createSection(title, rows, collapsed) {
    const sectionId = "sec_" + Math.random().toString(36).substr(2, 9);
    const displayStyle = collapsed ? "none" : "block";

    let html = `
        <div style="margin-top:20px; border:1px solid #ccc; border-radius:8px;">
            
            <!-- HEADER -->
            <div onclick="toggleSection('${sectionId}')"
                 style="cursor:pointer; padding:10px; font-weight:bold; background:#eee;">
                ${title} (${rows.length})
            </div>

            <!-- CONTENT -->
            <div id="${sectionId}" style="display:${displayStyle}; padding:10px;">
    `;

    if (rows.length === 0) {
        html += "<p>Nessun record</p>";
    } else {

        html += `
            <table style="width:100%; border-collapse:collapse;">
                <tr>
                    <th>Codice Riga</th>
                    <th>Articolo</th>
                    <th>Descrizione</th>
                    <th>Richiesta</th>
                    <th>Confezionata</th>
                    <th>Residua</th>
                    <th>Anomalia</th>
                </tr>
        `;

        rows.forEach(row => {

            const isAnomalia = row.qtaResidua != 0 || row.attivitaNonDichiarata === 'Y';
            const bgColor = isAnomalia ? "#f8d7da" : "#d4edda";

            let descrizioneAnomalia = "";
            if (row.attivitaNonDichiarata === 'Y') {
                descrizioneAnomalia = "Attivita non dichiarata";
            }
            if (row.qtaResidua != 0) {
                descrizioneAnomalia += "<br/>  Quantita residua";
            }

            html += `
                <tr style="background:${bgColor};">
                    <td style="border-top:1px solid #ccc; border-bottom:1px solid #ccc;">${row.codiceRiga}</td>
                    <td style="border-top:1px solid #ccc; border-bottom:1px solid #ccc;">${row.codArticolo}</td>
                    <td style="border-top:1px solid #ccc; border-bottom:1px solid #ccc;">${row.descManuale}</td>
                    <td style="border-top:1px solid #ccc; border-bottom:1px solid #ccc;">${row.qtaRichiesta}</td>
                    <td style="border-top:1px solid #ccc; border-bottom:1px solid #ccc;">${row.qtaConfezionata}</td>
                    <td style="border-top:1px solid #ccc; border-bottom:1px solid #ccc;"><b>${row.qtaResidua}</b></td>
                    <td style="border-top:1px solid #ccc; border-bottom:1px solid #ccc;"><b>${descrizioneAnomalia}</b></td>
                </tr>
            `;
        });

        html += "</table>";
    }

    html += `</div></div>`;

    return html;
}


// Toggle function
function toggleSection(id) {
    const el = document.getElementById(id);
    el.style.display = (el.style.display === "none") ? "block" : "none";
}
