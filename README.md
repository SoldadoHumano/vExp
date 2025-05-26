# vExp

vExp é um plugin open source para servidores **Minecraft Spigot/Paper** que permite aos jogadores **gerenciar experiência (XP)** com comandos avançados. Você pode verificar seu XP, dar XP para outros jogadores, armazenar XP em frascos e muito mais!

## 📦 Funcionalidades

- Consultar seu próprio XP e nível.
- Verificar o XP de outros jogadores.
- Criar frascos de XP com toda ou parte da sua experiência.
- Dar ou definir XP (em pontos ou níveis) para outros jogadores.
- Limpar toda a experiência de um jogador com confirmação.
- Sistema de mensagens totalmente configurável via `messages.yml`.

---

## 💬 Comandos

### `/xp`
Mostra o seu XP, nível atual e quanto falta para o próximo nível.

#### `/xp <jogador>`
Mostra o XP e o nível de outro jogador.

#### `/xp give <jogador> <quantia> <level|points>`
Dá XP para outro jogador. (Comando de staff)

#### `/xp set <jogador> <quantia> <level|points>`
Define o XP de outro jogador. (Comando de staff)

#### `/xp clear <jogador>`
Solicita confirmação para limpar o XP do jogador. (Comando de staff)

#### `/xp confirm <jogador>`
Confirma a limpeza de XP solicitada anteriormente. 

### `/frasco <quantia>`
Armazena a quantidade especificada de XP em um frasco de XP.

### `/frasco all`
Armazena **todo** o XP atual do jogador em um frasco.

---

## 🔐 Permissões

Permissão | Descrição 
`vexp.use` | Permite o uso dos comandos `/xp`, `/frasco` e ver XP de outros. (Por padrão vem como default no `plugin.yml`)
`vexp.staff` | Permite usar comandos administrativos (`give`, `set`, `clear`). 

---

## 🛠️ Configuração

O plugin gera automaticamente um arquivo `messages.yml` com todas as mensagens configuráveis. Você pode editar esse arquivo para personalizar todas as mensagens exibidas aos jogadores.

---

## 🧪 Requisitos

- Servidor **Spigot** ou **Paper**
- Versão compatível com API do Bukkit usada no plugin (1.8.8)

---

## 📄 Licença

Este projeto é de **uso livre**. Nenhuma licença específica foi aplicada, o que significa que você pode **modificar, redistribuir e utilizar como quiser**. Uma menção ao autor é apreciada, mas não obrigatória.

---

## 👨‍💻 Autor

Desenvolvido por **Vitor**  
> _Plugin criado com carinho e dedicação._

---

## 📫 Discord / Contato

Discord: [vitor1227_OP Community](https://discord.gg/Mkjs7GH3Br)
