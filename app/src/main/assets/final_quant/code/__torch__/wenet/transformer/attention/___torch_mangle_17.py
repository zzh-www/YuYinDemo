class RelPositionMultiHeadedAttention(Module):
  __parameters__ = ["pos_bias_u", "pos_bias_v", ]
  __buffers__ = []
  pos_bias_u : Tensor
  pos_bias_v : Tensor
  training : bool
  d_k : int
  h : int
  linear_q : __torch__.torch.nn.quantized.dynamic.modules.linear.Linear
  linear_k : __torch__.torch.nn.quantized.dynamic.modules.linear.Linear
  linear_v : __torch__.torch.nn.quantized.dynamic.modules.linear.Linear
  linear_out : __torch__.torch.nn.quantized.dynamic.modules.linear.Linear
  dropout : __torch__.torch.nn.modules.dropout.___torch_mangle_3.Dropout
  linear_pos : __torch__.torch.nn.quantized.dynamic.modules.linear.Linear
  def forward(self: __torch__.wenet.transformer.attention.___torch_mangle_17.RelPositionMultiHeadedAttention,
    query: Tensor,
    key: Tensor,
    value: Tensor,
    mask: Optional[Tensor],
    pos_emb: Tensor) -> Tensor:
    _0 = (self).forward_qkv(query, key, value, )
    q, k, v, = _0
    q0 = torch.transpose(q, 1, 2)
    n_batch_pos = torch.size(pos_emb, 0)
    _1 = (self.linear_pos).forward(pos_emb, )
    p = torch.view(_1, [n_batch_pos, -1, self.h, self.d_k])
    p0 = torch.transpose(p, 1, 2)
    _2 = torch.add(q0, self.pos_bias_u, alpha=1)
    q_with_bias_u = torch.transpose(_2, 1, 2)
    _3 = torch.add(q0, self.pos_bias_v, alpha=1)
    q_with_bias_v = torch.transpose(_3, 1, 2)
    matrix_ac = torch.matmul(q_with_bias_u, torch.transpose(k, -2, -1))
    matrix_bd = torch.matmul(q_with_bias_v, torch.transpose(p0, -2, -1))
    _4 = torch.add(matrix_ac, matrix_bd, alpha=1)
    scores = torch.div(_4, torch.sqrt(self.d_k))
    _5 = (self).forward_attention(v, scores, mask, )
    return _5
  def forward_qkv(self: __torch__.wenet.transformer.attention.___torch_mangle_17.RelPositionMultiHeadedAttention,
    query: Tensor,
    key: Tensor,
    value: Tensor) -> Tuple[Tensor, Tensor, Tensor]:
    n_batch = torch.size(query, 0)
    q = torch.view((self.linear_q).forward(query, ), [n_batch, -1, self.h, self.d_k])
    k = torch.view((self.linear_k).forward(key, ), [n_batch, -1, self.h, self.d_k])
    v = torch.view((self.linear_v).forward(value, ), [n_batch, -1, self.h, self.d_k])
    q1 = torch.transpose(q, 1, 2)
    k0 = torch.transpose(k, 1, 2)
    v0 = torch.transpose(v, 1, 2)
    return (q1, k0, v0)
  def forward_attention(self: __torch__.wenet.transformer.attention.___torch_mangle_17.RelPositionMultiHeadedAttention,
    value: Tensor,
    scores: Tensor,
    mask: Optional[Tensor]) -> Tensor:
    n_batch = torch.size(value, 0)
    if torch.__isnot__(mask, None):
      mask0 = unchecked_cast(Tensor, mask)
      mask1 = torch.eq(torch.unsqueeze(mask0, 1), 0)
      scores0 = torch.masked_fill(scores, mask1, -inf)
      attn0 = torch.masked_fill(torch.softmax(scores0, -1, None), mask1, 0.)
      attn = attn0
    else:
      attn = torch.softmax(scores, -1, None)
    p_attn = (self.dropout).forward(attn, )
    x = torch.matmul(p_attn, value)
    _6 = torch.contiguous(torch.transpose(x, 1, 2), memory_format=0)
    _7 = [n_batch, -1, torch.mul(self.h, self.d_k)]
    x0 = torch.view(_6, _7)
    return (self.linear_out).forward(x0, )
