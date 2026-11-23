# MasterWave Email Compliance Guide (CAN-SPAM, GDPR & RFC 8058)

To avoid legal penalties under the U.S. CAN-SPAM Act (15 U.S.C. §§ 7701-7713) — which carries fines up to **$51,744 per violating email** — and to satisfy EU GDPR Article 21, every marketing, promotional, or announcement email sent on behalf of MasterWave MUST adhere to these rules:

---

## 1. Required MIME Headers (RFC 8058 1-Click Unsubscribe)
All major mailbox providers (Google Gmail, Yahoo Mail, Apple Mail, Microsoft Outlook) require one-click list unsubscribe headers for bulk senders. When constructing an email, include these exact headers:

```http
From: "MasterWave Audio" <updates@masterwave.vercel.app>
To: {{USER_EMAIL}}
Subject: MasterWave 2.2 Release: Lossless 32-Bit Sound & Liquid Glass
List-Unsubscribe: <https://masterwave.vercel.app/api/unsubscribe?id={{USER_ID}}&token={{TOKEN}}>, <mailto:unsubscribe@masterwave.vercel.app?subject=unsubscribe:{{USER_ID}}>
List-Unsubscribe-Post: List-Unsubscribe=One-Click
Precedence: bulk
Feedback-ID: updates:masterwave:broadcast
```

---

## 2. Mandatory Footer Elements
Every marketing email body MUST visibly contain:

1. **Sender Physical Postal Address (15 U.S.C. § 7704(a)(5)(A)):**
   ```text
   MasterWave Audio Technologies
   100 Pine Street, Suite 1250
   San Francisco, CA 94111, USA
   ```
   *(Note: This can be a commercial registered agent office, CMRA mailbox, or company headquarters. A pure P.O. Box must be registered with USPS).*

2. **Clear and Conspicuous Opt-Out Link:**
   - Link text: `"Unsubscribe instantly"` or `"Manage email preferences"`.
   - Destination: Direct link to `https://masterwave.vercel.app/unsubscribe`.
   - Must NOT require the user to log in or pay a fee to opt out.
   - Opt-out requests must be honored within **10 business days** (MasterWave's web endpoint honors it instantly).

3. **Notice of Advertisement / Purpose:**
   - Clearly state why the user received the email (e.g., `"You received this email because you opted in to MasterWave release announcements"`).

4. **Accurate Header & Subject Lines (No Deceptive Headers):**
   - The `From` name and email domain must match MasterWave.
   - The `Subject` line must accurately describe the content of the message.
