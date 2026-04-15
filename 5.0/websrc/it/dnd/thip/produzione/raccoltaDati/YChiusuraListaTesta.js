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

}

function conferma() {
	document.getElementById("spinner").style.display = "flex";//72435 mostro lo spinner
	var className = eval("document.forms[0].thClassName.value");
	runActionDirect('CONFERMA_CHIUSURA_LISTA_TESTA', 'action_submit', className, null, 'same', 'no');
}