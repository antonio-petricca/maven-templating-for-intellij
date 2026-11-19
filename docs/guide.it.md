# Maven Templating assistant: guida utente

English version: [guide.md](https://github.com/antonio-petricca/maven-templating-for-intellij/blob/main/docs/guide.md).

## Cosa fa il plugin

Il [templating-maven-plugin](https://www.mojohaus.org/templating-maven-plugin/) ufficiale filtra i file Java collocati in una cartella di template (per default `src/main/java-templates` e `src/test/java-templates`) e genera i sorgenti veri nella cartella di build. IntelliJ IDEA non sa che queste cartelle contengono sorgenti: bisogna marcarle a mano come **Sources Root** o **Test Sources Root**, e l'IDE spesso perde la marcatura quando il progetto Maven viene reimportato.

**Maven Templating assistant** lo fa al posto tuo:

- dichiara le cartelle dei template come radici dei sorgenti durante l'import di Maven, leggendole dal `pom.xml`, così restano dopo ogni reimport;
- mantiene marcate le cartelle quando le crei, rinomini, sposti, copi o cancelli;
- può escludere i sorgenti generati per evitare classi duplicate;
- permette di passare da un template al suo sorgente generato e viceversa;
- completa, risolve e ispeziona le proprietà Maven `${...}` usate nei template.

## Requisiti e installazione

- IntelliJ IDEA dalla 2026.2 (build 262) alla 2026.3 (build 263.*). Le versioni precedenti non sono supportate.
- I plugin inclusi **Maven** e **Java** devono essere attivi.

Installazione dal Marketplace: **Impostazioni** > **Plugin** > **Marketplace**, cerca "Maven Templating assistant" e premi **Install**.

Installazione da disco: scarica l'[ultima release](https://github.com/antonio-petricca/maven-templating-for-intellij/releases/latest), poi **Impostazioni** > **Plugin** > icona dell'ingranaggio > **Install plugin from disk...**.

## Come vengono individuate le cartelle dei template

### Moduli che dichiarano il plugin

Quando il `pom.xml` (compresa la configurazione ereditata) dichiara `org.codehaus.mojo:templating-maven-plugin`, le cartelle si leggono dalla sua configurazione. Senza configurazione personalizzata valgono i default:

| Tipo | Cartella di default |
|---|---|
| Sorgenti dei template | `src/main/java-templates` |
| Sorgenti di test dei template | `src/test/java-templates` |

Puoi impostare cartelle personalizzate con `sourceDirectory` (goal `filter-sources`) e `testSourceDirectory` (goal `filter-test-sources`), a livello di plugin o dentro una execution associata a quel goal. Tutti i valori trovati vengono usati.

```xml
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>templating-maven-plugin</artifactId>
  <configuration>
    <sourceDirectory>${basedir}/src/main/my-templates</sourceDirectory>
  </configuration>
  <executions>
    <execution>
      <id>filter-test-sources</id>
      <goals>
        <goal>filter-test-sources</goal>
      </goals>
      <configuration>
        <testSourceDirectory>src/test/my-test-templates</testSourceDirectory>
      </configuration>
    </execution>
  </executions>
</plugin>
```

I percorsi relativi sono risolti rispetto alla cartella del modulo, e sono supportati `${basedir}`, `${project.basedir}` e `${project.build.directory}`. Per questi moduli il nome della cartella nelle impostazioni non viene usato.

### Moduli senza il plugin

Se il plugin non è dichiarato, si ripiega sul nome della cartella impostato nelle impostazioni (default `java-templates`): `src/main/<nome>` viene marcata come radice dei sorgenti e `src/test/<nome>` come radice dei sorgenti di test, ma solo se la cartella esiste.

### Quando modifichi le cartelle nell'IDE

Il plugin osserva il file system per le cartelle dei template di ogni modulo Maven:

| Operazione | Risultato |
|---|---|
| Creare una cartella di template | Viene marcata come radice dei sorgenti o dei sorgenti di test. |
| Rinominarla o spostarla | Viene smarcata nel vecchio posto e marcata nel nuovo, se anche questo è una cartella di template; altrimenti smette di essere una radice dei sorgenti. |
| Copiarla | La copia viene marcata se è una cartella di template. |
| Cancellarla | La radice dei sorgenti viene rimossa. |

## Scansione del progetto

La scansione cerca le cartelle dei template di ogni modulo Maven e marca quelle che esistono.

- **All'avvio**: parte all'apertura del progetto se **Scansiona il progetto all'avvio** è attivo (default). Non viene mostrata alcuna notifica.
- **Manualmente**: esegui **Scansiona il progetto per i template** (Scan Project for Templates) da **Strumenti** > **Maven Templating**, oppure dal menu contestuale della vista **Project**. L'azione è disponibile solo nei progetti Maven.
- **Dopo aver cambiato il nome della cartella** nelle impostazioni: parte una scansione che prima smarca le cartelle col vecchio nome.

Al termine di una scansione che hai richiesto, una notifica indica quante cartelle di template sono state marcate (oppure che erano già aggiornate) e, se ce ne sono, quante smarcate. Un indicatore di avanzamento mostra il modulo Maven in scansione. Le richieste ravvicinate vengono unite in una sola scansione.

## Impostazioni

Apri **Impostazioni** > **Compilazione, esecuzione, distribuzione** > **Strumenti di build** > **Maven Templating assistant**. La pagina ha il link **Apri la guida** (Open guide), che apre la guida online nel browser. Le impostazioni sono salvate per progetto, tranne **Mostra le novità dopo gli aggiornamenti**, che vale per tutti i progetti.

| Opzione | Default | Effetto | Quando ha effetto |
|---|---|---|---|
| Percorso dei template Java | `java-templates` | Nome della cartella sotto `src/main` e `src/test` per i moduli senza il plugin. Non può essere vuoto, `.` o `..`, iniziare o finire con spazi, né contenere `/` o `\`. | All'applicazione: parte una scansione che prima smarca le cartelle col vecchio nome. |
| Scansiona il progetto all'avvio | attiva | Scansiona il progetto a ogni apertura. | Alla prossima apertura del progetto. |
| Escludi i sorgenti generati dai template | disattiva | Esclude le cartelle di output generate, vedi la sezione successiva. | Dopo un reimport di Maven. All'applicazione, una notifica offre il pulsante **Reimporta**. |
| Mostra le novità dopo gli aggiornamenti | attiva | Apre il tab **Novità** (What's New) dopo un aggiornamento del plugin (e dopo la prima installazione). Vale per tutti i progetti. | Dal prossimo aggiornamento. |

## Esclusione dei sorgenti generati

Il plugin genera copie dei template nella cartella di build, quindi le stesse classi esistono due volte e l'IDE può segnalare errori **Duplicate class**. Con **Escludi i sorgenti generati dai template** attivo, quelle cartelle di output vengono escluse dal progetto:

- `target/generated-sources/java-templates` e `target/generated-test-sources/java-templates` (default);
- le cartelle impostate con `outputDirectory` (goal `filter-sources`) e `testOutputDirectory` (goal `filter-test-sources`), a livello di plugin o di execution.

L'esclusione è dichiarata durante l'import di Maven, quindi ha effetto solo dopo un reimport; la notifica mostrata quando cambi l'opzione ha un pulsante **Reimporta**. Vale per i moduli che dichiarano il plugin.

## Navigazione tra template e sorgenti generati

Per un file Java in una cartella di template, il plugin trova il file generato con lo stesso percorso relativo nella cartella di output, e viceversa. Il file generato deve esistere su disco, quindi compila prima il progetto.

- **Icona nel margine** (gutter) accanto al nome della classe di primo livello, con il tooltip **Vai al sorgente generato** (Go to Generated Source) o **Vai al template** (Go to Template).
- **Naviga** > **Vai al sorgente generato / Vai al template**.
- La stessa voce nel menu contestuale dell'editor.

L'azione è visibile solo quando esiste una controparte.

## Proprietà Maven nei template

Nei file Java di una cartella di template, le occorrenze `${nome}` dentro stringhe e commenti sono trattate come proprietà Maven.

- **Risoluzione**: Ctrl+clic su una proprietà porta alla sua definizione. Le proprietà si cercano nelle `<properties>` del pom del modulo e dei pom parent (vince il più vicino), poi fra le proprietà del modello `project.version`, `project.groupId`, `project.artifactId`, `project.name`, `project.description`, `project.basedir` e `project.build.directory`, poi fra quelle già risolte da Maven (profili, file di configurazione e così via, senza un punto in cui andare).
- **Completamento**: dopo aver digitato `${`, la lista propone le proprietà dichiarate nel pom (indicate `pom.xml`) e quelle del modello o già risolte (indicate `Modello Maven`).
- **Ispezione**: l'ispezione **Proprietà Maven non risolta nel template** (Unresolved Maven property in template; avviso, attiva di default, gruppo **Maven Templating assistant**) segnala le proprietà non trovate, con il messaggio `Impossibile risolvere la proprietà Maven 'nome'`. Le proprietà ancora in digitazione (senza parentesi graffa di chiusura) non vengono segnalate.
- **Quick fix**: **Aggiungi la proprietà al pom.xml** (Add property to pom.xml) aggiunge un tag `<nome/>` vuoto alle `<properties>` del pom del modulo (creando la sezione se serve), pronto da compilare.
- **Proprietà ignorate**: i nomi che iniziano con `env.`, `java.`, `os.`, `user.`, `settings.`, `maven.`, `project.` o `pom.`, `basedir` e qualunque proprietà di sistema della JVM non vengono mai segnalati.

## Tab Novità

Dopo l'aggiornamento del plugin si apre un tab **Novità** con le novità della versione, mostrate nella lingua dell'IDE, e un link che apre questa guida inclusa nel plugin. Il tab si apre anche dopo la prima installazione, insieme a una notifica con l'azione **Apri la guida**. Entrambi non compaiono se **Mostra le novità dopo gli aggiornamenti** è disattivata. Puoi aprire il tab anche da **Strumenti** > **Maven Templating** > **Novità**.

## Risoluzione dei problemi e FAQ

**Una cartella di template non è marcata.**
Esegui **Strumenti** > **Maven Templating** > **Scansiona il progetto per i template**, oppure reimporta il progetto Maven. Verifica che la cartella esista, che corrisponda al percorso nella configurazione del plugin (o al nome nelle impostazioni, per i moduli senza il plugin) e che appartenga a un modulo Maven.

**Ottengo errori "Duplicate class".**
Attiva **Escludi i sorgenti generati dai template** nelle impostazioni e reimporta il progetto Maven.

**Il plugin non fa nulla nel mio progetto.**
Funziona solo con i progetti Maven: altrimenti l'azione di scansione è nascosta.

**La pagina delle impostazioni e i messaggi sono in italiano.**
Succede quando l'IDE usa il language pack italiano.

**Dove trovo la guida?**
In **Aiuto** > **Guida di Maven Templating** (Maven Templating Guide), **Strumenti** > **Maven Templating** > **Apri la guida**, oppure nel link **Apri la guida** delle impostazioni.

**È sicuro con le prossime versioni dell'IDE?**
Per dichiarare le cartelle durante l'import il plugin usa API sperimentali del plugin Maven. Sono verificate con il Plugin Verifier a ogni release supportata, per questo l'intervallo di compatibilità è limitato.

## Link

- Repository: https://github.com/antonio-petricca/maven-templating-for-intellij
- Issue tracker: https://github.com/antonio-petricca/maven-templating-for-intellij/issues
- Changelog: [CHANGELOG.it.md](https://github.com/antonio-petricca/maven-templating-for-intellij/blob/main/CHANGELOG.it.md)
