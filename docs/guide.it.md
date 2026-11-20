# Maven Templating assistant: guida utente

In breve: Maven Templating assistant fa sì che IntelliJ IDEA tratti come sorgenti le cartelle dei template del templating-maven-plugin, le mantiene tali dopo ogni reimport di Maven e ti aiuta a lavorare con i template, i sorgenti generati e le proprietà Maven che usano.

English version: [guide.md](https://github.com/antonio-petricca/maven-templating-for-intellij/blob/main/docs/guide.md).

## Dove trovare le funzioni

| Dove | Cosa fa |
|---|---|
| Tasto destro nell'editor, su un tab dell'editor o nella vista **Project** > **Maven Templating** | Vai al sorgente generato / Vai al template (Go to Generated Source / Go to Template), Cerca le cartelle dei template (Scan Project for Templates), Impostazioni (Settings), Apri la guida (Open Guide), Novità (What's New). Il sottomenu è sempre presente; in un progetto non importato da Maven, Cerca le cartelle dei template è disabilitata. |
| Icona nel margine dell'editor, accanto al nome della classe di primo livello | Porta al sorgente generato, o torna al template. |
| Banner in cima a un template o a un sorgente generato | Stesso salto, con un link. |
| Ctrl+Alt+Shift+Space (Windows/Linux) o Cmd+Alt+Shift+G (macOS) | Vai al sorgente generato / Vai al template. |
| **Navigate** > **Vai al sorgente generato / Vai al template**, e il sottomenu **Go To** del menu del tasto destro dell'editor | Lo stesso salto, accanto a **Related Symbol**. |
| **Tools** > **Maven Templating** | Le stesse voci del sottomenu del tasto destro. |
| **Help** > **Maven Templating Guide** | Apre la guida in una finestra del plugin. |
| **Settings** > **Build, Execution, Deployment** > **Build Tools** > **Maven Templating assistant** | Nome della cartella per i moduli senza il plugin, scansione all'avvio, esclusione dei sorgenti generati, novità dopo gli aggiornamenti, link a questa guida. |
| **Tools** > **Maven Templating** > **Novità** | Riapre il tab **Novità** (What's New). |

## Cosa fa il plugin

Il [templating-maven-plugin](https://www.mojohaus.org/templating-maven-plugin/) ufficiale filtra i file Java collocati in una cartella di template (per default `src/main/java-templates` e `src/test/java-templates`) e genera i sorgenti veri nella cartella di build. IntelliJ IDEA non sa che queste cartelle contengono sorgenti: bisogna contrassegnarle a mano come **Sources Root** o **Test Sources Root**, e l'IDE spesso perde l'impostazione quando il progetto Maven viene reimportato.

**Maven Templating assistant** lo fa al posto tuo:

- dichiara le cartelle dei template come radici dei sorgenti durante l'import di Maven, leggendole dal `pom.xml`, così restano dopo ogni reimport;
- mantiene contrassegnate le cartelle quando le crei, rinomini, sposti, copi o cancelli;
- esclude i sorgenti generati (attivo di default) per evitare classi duplicate;
- permette di passare da un template al suo sorgente generato e viceversa;
- completa, risolve e ispeziona le proprietà Maven `${...}` usate nei template.

## Requisiti e installazione

- IntelliJ IDEA dalla 2026.2 (build 262) alla 2026.3 (build 263.*). Le versioni precedenti non sono supportate.
- I plugin inclusi **Maven** e **Java** devono essere attivi.

Installazione dal Marketplace: **Settings** > **Plugins** > **Marketplace**, cerca "Maven Templating assistant" e premi **Install**.

Installazione da disco: scarica l'[ultima release](https://github.com/antonio-petricca/maven-templating-for-intellij/releases/latest), poi **Settings** > **Plugins** > icona dell'ingranaggio > **Install plugin from disk...**.

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

I percorsi relativi sono risolti rispetto alla cartella del modulo, e sono supportati `${basedir}`, `${project.basedir}` e `${project.build.directory}`. Per questi moduli il nome della cartella nelle impostazioni non viene usato: decide il pom.

### Moduli senza il plugin

Se il plugin non è dichiarato, si ripiega sul nome della cartella impostato nelle impostazioni, **Nome della cartella dei template (moduli senza il plugin)** (default `java-templates`): `src/main/<nome>` viene contrassegnata come radice dei sorgenti e `src/test/<nome>` come radice dei sorgenti di test, ma solo se la cartella esiste.

### Quando modifichi le cartelle nell'IDE

Il plugin osserva il file system per le cartelle dei template di ogni modulo Maven:

| Operazione | Risultato |
|---|---|
| Creare una cartella di template | Viene contrassegnata come radice dei sorgenti o dei sorgenti di test. |
| Rinominarla o spostarla | Non è più contrassegnata nel vecchio posto e viene contrassegnata nel nuovo, se anche questo è una cartella di template; altrimenti smette di essere una radice dei sorgenti. |
| Copiarla | La copia viene contrassegnata se è una cartella di template. |
| Cancellarla | La radice dei sorgenti viene rimossa. |

## Scansione del progetto

La scansione cerca le cartelle dei template di ogni modulo Maven e marca quelle che esistono.

- **All'avvio**: parte all'apertura del progetto se **Scansiona il progetto all'avvio** è attivo (default). Non viene mostrata alcuna notifica.
- **Manualmente**: esegui **Cerca le cartelle dei template** da **Tools** > **Maven Templating**, oppure dal sottomenu **Maven Templating** del menu del tasto destro dell'editor, di un tab dell'editor o della vista **Project**. L'azione è disponibile solo nei progetti importati da Maven: negli altri resta nel menu ma è disabilitata, e la sua descrizione ne spiega il motivo.
- **Dopo aver cambiato il nome della cartella** nelle impostazioni: parte una scansione che prima smarca le cartelle col vecchio nome.

Al termine di una scansione che hai richiesto, una notifica con titolo **Maven Templating assistant** dice, per esempio, "1 cartella di template contrassegnata come cartella dei sorgenti" oppure "Cartelle di template già aggiornate" e, se ce ne sono state, "1 cartella di template non più contrassegnata come cartella dei sorgenti". Un indicatore di avanzamento mostra il modulo Maven in scansione. Le richieste ravvicinate vengono unite in una sola scansione.

## Impostazioni

Apri **Settings** > **Build, Execution, Deployment** > **Build Tools** > **Maven Templating assistant**. La pagina ha il link **Apri la guida** (Open guide), che apre questa guida in una finestra del plugin. La pagina si apre anche dal sottomenu **Maven Templating**, con **Impostazioni**. Le impostazioni sono salvate per progetto, tranne **Mostra le novità dopo gli aggiornamenti**, che vale per tutti i progetti.

| Opzione | Default | Effetto | Quando ha effetto |
|---|---|---|---|
| Nome della cartella dei template (moduli senza il plugin) | `java-templates` | Nome della cartella sotto `src/main` e `src/test`, usato solo dai moduli che non dichiarano il plugin (per gli altri decide il pom). Non può essere vuoto, `.` o `..`, iniziare o finire con spazi, né contenere `/` o `\`. | All'applicazione: parte una scansione che prima smarca le cartelle col vecchio nome. |
| Scansiona il progetto all'avvio | attiva | Scansiona il progetto a ogni apertura. | Alla prossima apertura del progetto. |
| Escludi i sorgenti generati dai template | attiva | Esclude solo le cartelle di output del templating-maven-plugin, nei moduli che lo dichiarano; vedi la sezione successiva. | Dopo un reimport di Maven. All'applicazione, una notifica offre il pulsante **Reimporta**. |
| Mostra le novità dopo gli aggiornamenti | attiva | Apre il tab **Novità** (What's New) dopo un aggiornamento del plugin (e dopo la prima installazione). Vale per tutti i progetti. | Dal prossimo aggiornamento. |

## Esclusione dei sorgenti generati

Il plugin genera copie dei template nella cartella di build, quindi le stesse classi esistono due volte e l'IDE può segnalare errori **Duplicate class**. **Escludi i sorgenti generati dai template** è attivo di default: esclude solo le cartelle di output del templating-maven-plugin, e solo nei moduli che dichiarano il plugin. Nient'altro sotto `target` viene toccato. Le cartelle escluse sono:

- `target/generated-sources/java-templates` e `target/generated-test-sources/java-templates` (default);
- le cartelle impostate con `outputDirectory` (goal `filter-sources`) e `testOutputDirectory` (goal `filter-test-sources`), a livello di plugin o di execution.

L'esclusione è dichiarata durante l'import di Maven, quindi ha effetto solo dopo un reimport; la notifica mostrata quando cambi l'opzione ha un pulsante **Reimporta**.

## Navigazione tra template e sorgenti generati

Per un file Java in una cartella di template, il plugin trova il file generato con lo stesso percorso relativo nella cartella di output, e viceversa. Il file generato deve esistere su disco, quindi compila prima il progetto (vedi sotto).

- **Icona nel margine** (gutter) accanto al nome della classe di primo livello, con il tooltip **Vai al sorgente generato** (Go to Generated Source) o **Vai al template** (Go to Template). Le due direzioni hanno icone diverse: un riquadro arancione con `${}` e una freccia a destra sul template, un cerchio blu con la "C" di una classe Java e una freccia a sinistra sul sorgente generato.
- **Banner** in alto nell'editor, sia nel template sia nel sorgente generato, con un link all'altro file. Sul sorgente generato dice "Generato dal templating-maven-plugin a partire da un template." con il link **Apri il template**; sul template, "Questo template viene trasformato in un sorgente generato dal templating-maven-plugin." con il link **Apri il sorgente generato**.
- **Navigate** > **Vai al sorgente generato / Vai al template**, accanto a **Related Symbol**, e la stessa voce nel sottomenu **Go To** del menu del tasto destro dell'editor.
- **Scorciatoia** Ctrl+Alt+Shift+Space su Windows e Linux, Cmd+Alt+Shift+G su macOS. Si può cambiare in **Settings** > **Keymap**.
- Il sottomenu **Maven Templating** nei menu del tasto destro: nell'editor, sul tab dell'editor e sul file nella vista **Project**. Qui la voce è disabilitata su un file che non è né un template né un sorgente generato, mentre nei menu **Navigate** e **Go To** è nascosta.

### Prima della prima build

Il file generato esiste solo dopo una build. Finché manca, il template mostra il banner "Ancora nessun sorgente generato: compila il progetto (per esempio mvn generate-sources) per poterlo aprire." e **Vai al sorgente generato** resta visibile ma disabilitata. Compila il progetto (per esempio `mvn generate-sources`) e il salto diventa disponibile. Sul sorgente generato l'azione è visibile solo se il suo template esiste.

## Proprietà Maven nei template

Nei file Java di una cartella di template, le occorrenze `${nome}` dentro stringhe e commenti sono trattate come proprietà Maven.

- **Risoluzione**: Ctrl+clic su una proprietà porta alla sua definizione. Le proprietà si cercano nelle `<properties>` del pom del modulo e dei pom parent (vince il più vicino), poi fra le proprietà del modello `project.version`, `project.groupId`, `project.artifactId`, `project.name`, `project.description`, `project.basedir` e `project.build.directory`, poi fra quelle già risolte da Maven (profili, file di configurazione e così via, senza un punto in cui andare).
- **Completamento**: dopo aver digitato `${`, la lista propone le proprietà dichiarate nel pom (indicate `pom.xml`) e quelle del modello o già risolte (indicate `Modello Maven`).
- **Ispezione**: l'ispezione **Proprietà Maven non risolta nel template** (Unresolved Maven property in template; avviso, attiva di default, gruppo **Maven Templating assistant**) segnala le proprietà non trovate, con il messaggio `Impossibile risolvere la proprietà Maven 'nome'`. Le proprietà ancora in digitazione (senza parentesi graffa di chiusura) non vengono segnalate.
- **Quick fix**: **Aggiungi la proprietà al pom.xml** (Add property to pom.xml) aggiunge un tag `<nome/>` vuoto alle `<properties>` del pom del modulo (creando la sezione se serve), poi apre il pom con il cursore nel valore vuoto, pronto da compilare.
- **Proprietà ignorate**: `basedir`, i nomi che iniziano con `env.`, `java.`, `os.`, `user.`, `settings.`, `maven.`, `project.` o `pom.` e qualunque proprietà di sistema della JVM non vengono mai segnalati.

## Tab Novità

Dopo l'aggiornamento del plugin si apre un tab **Novità** (What's New). In alto mostra la grande icona del plugin e un saluto: "Grazie per aver installato Maven Templating assistant!" dopo la prima installazione, "Grazie per l'aggiornamento!" dopo un aggiornamento. Seguono una breve descrizione dello scopo del plugin, la sezione **Novità della versione** con il numero di versione e le novità di quella versione nella lingua dell'IDE, la sezione **Dove trovare le funzioni** e, in fondo, un link al changelog completo su GitHub. La pagina prende colori e font dal tema dell'IDE, chiaro o scuro. Il tab non contiene link a questa guida e non viene mostrata alcuna notifica. Non compare se **Mostra le novità dopo gli aggiornamenti** è disattivata, impostazione che vale per tutti i progetti. Puoi aprire il tab anche da **Tools** > **Maven Templating** > **Novità**.

## Risoluzione dei problemi e FAQ

**Una cartella di template non è contrassegnata come sorgente.**
Esegui **Tools** > **Maven Templating** > **Cerca le cartelle dei template**, oppure reimporta il progetto Maven. Verifica che la cartella esista, che corrisponda al percorso nella configurazione del plugin (o al nome nelle impostazioni, per i moduli senza il plugin) e che appartenga a un modulo Maven.

**Ottengo errori "Duplicate class".**
Verifica che **Escludi i sorgenti generati dai template** sia attivo nelle impostazioni (lo è di default) e reimporta il progetto Maven. L'esclusione vale solo per i moduli che dichiarano il templating-maven-plugin.

**Il plugin non fa nulla nel mio progetto.**
Funziona solo con i progetti Maven. Il sottomenu **Maven Templating** è sempre presente, ma negli altri progetti Cerca le cartelle dei template è disabilitata.

**La pagina delle impostazioni e i messaggi sono in italiano.**
I testi italiani del plugin compaiono solo quando anche l'IDE è in italiano, cosa che richiede un language pack di terze parti (JetBrains non ne distribuisce uno). Altrimenti tutto è in inglese, e i menu dell'IDE restano comunque in inglese.

**Vai al sorgente generato è disabilitata.**
Il sorgente generato non esiste ancora: compila il progetto (per esempio `mvn generate-sources`).

**Dove trovo la guida?**
In **Help** > **Maven Templating Guide**, **Tools** > **Maven Templating** > **Apri la guida**, oppure nel link **Apri la guida** delle impostazioni.

**È sicuro con le prossime versioni dell'IDE?**
Per dichiarare le cartelle durante l'import il plugin usa API sperimentali del plugin Maven. Sono verificate con il Plugin Verifier a ogni release supportata, per questo l'intervallo di compatibilità è limitato.

## Link

- Repository: https://github.com/antonio-petricca/maven-templating-for-intellij
- Issue tracker: https://github.com/antonio-petricca/maven-templating-for-intellij/issues
- Changelog: [CHANGELOG.it.md](https://github.com/antonio-petricca/maven-templating-for-intellij/blob/main/CHANGELOG.it.md)
