# Documentação: Melhorias no Registro de Consumo

Este documento detalha as alterações realizadas no projeto **CafeSG** para aumentar a robustez e integridade do registro de consumos, especialmente em cenários de instabilidade de rede e falhas de envio de imagem.

## 🔍 Vulnerabilidades Identificadas
Anteriormente, o sistema apresentava os seguintes riscos:
- **Perda de Fotos**: Caso o registro do consumo na API funcionasse mas o envio da foto falhasse, a foto era descartada.
- **Sincronização Ineficiente**: Múltiplos processos de sincronização podiam ocorrer em paralelo se o usuário fizesse registros rápidos.
- **Tratamento Genérico de Erros**: Erros de negócio (ex: dispositivo desautorizado) eram salvos offline, causando tentativas infinitas de sincronização de dados inválidos.
- **Registros Duplicados**: Usuários podiam clicar múltiplas vezes no botão de confirmação antes do processamento da imagem terminar, gerando dois ou mais registros idênticos para o mesmo café.

---

## 🛠️ Alterações Técnicas Implementadas

### 1. Novo Modelo de Resposta (`Models.kt`)
Adição da flag `isOffline` ao objeto `ConsumoResponse`:
```kotlin
data class ConsumoResponse(
    val message: String,
    val id: Int,
    val isOffline: Boolean = false
)
```
- **Motivo**: Elimina a dependência de mensagens de texto ("strings mágicas") na ViewModel para identificar o estado do registro.

### 2. Controle de Sincronização Parcial (`ConsumoOfflineEntity.kt`)
A entidade de banco de dados local agora rastreia o estado de sincronização de forma granular:
- `serverId: Int?`: Armazena o ID retornado pela API caso o consumo seja registrado mas a foto falhe.
- `syncConsumoPending: Boolean`: Indica se os dados de texto do consumo ainda precisam ser enviados.
- `syncFotoPending: Boolean`: Indica se a imagem ainda precisa ser enviada.

### 3. Repositório Inteligente (`CafeRepository.kt`)
Implementação de lógica robusta de registro e sincronização:
- **`Mutex`**: Garante que apenas uma sincronização ocorra por vez (thread-safety).
- **Tratamento de Exceções**: 
  - `IOException` (Rede): Aciona salvamento offline.
  - `HttpException` (Negócio): Propaga o erro para a interface sem salvar offline.
- **Lógica de Reenvio de Foto**: Se registrar o consumo mas a foto falhar, o app salva o ID do servidor localmente e tentará apenas a foto no próximo sync.

### 4. Interface Reativa (`MainViewModel.kt`)
- Utiliza a flag `isOffline` para decidir o fluxo da UI.
- Exibe mensagens de erro específicas para falhas de autorização (403 Forbidden).

### 5. Banco de Dados (`AppDatabase.kt`)
- Atualização para a **Versão 3**.
- Mantido `fallbackToDestructiveMigration()` para garantir a atualização dos campos da entidade em dispositivos de teste.

### 6. Proteção Contra Clique Duplo (UI - `CafeSGUi.kt`)
Implementação de estado local `isProcessing` no componente `ValueSelectionCard`:
- **Bloqueio Imediato**: O botão "Confirmar" e todos os controles de valor são desabilitados **instantaneamente** ao primeiro toque.
- **Sincronismo com Câmera**: O bloqueio ocorre antes mesmo do disparo da câmera e do processamento da imagem em Base64, eliminando a janela de tempo onde múltiplos cliques eram possíveis.

### 7. Segurança de Domínio (Debounce 5s - `MainViewModel.kt`)
Implementação de uma "última linha de defesa" na lógica de negócio:
- **Verificação de Identidade e Valor**: A ViewModel rastreia o `codigo` do funcionário, o `valor` e o `timestamp` do último registro bem-sucedido.
- **Bloqueio de Repetição**: Se uma nova tentativa de registro para o **mesmo funcionário** e **mesmo valor** ocorrer em menos de **5 segundos**, a requisição é ignorada e um aviso é gerado nos logs (`Log.w`).

---

## ✅ Benefícios Alcançados
1. **Zero Perda de Dados**: Fotos nunca são descartadas em caso de falha de conexão.
2. **Prevenção de Duplicidade**: Eliminação total de registros repetidos causados por cliques acidentais ou ansiedade do usuário.
3. **Menor Carga no Servidor**: Evita registros duplicados de consumo em caso de falha parcial de rede.
4. **Melhor Experiência do Usuário (UX)**: O usuário recebe feedback preciso sobre o estado do registro (Online vs Offline) e erros de autorização, além de um botão que responde visualmente de forma imediata ao clique.
5. **Manutenibilidade**: Código desacoplado de mensagens de texto literais e protegido por camadas de segurança tanto na UI quanto na ViewModel.
