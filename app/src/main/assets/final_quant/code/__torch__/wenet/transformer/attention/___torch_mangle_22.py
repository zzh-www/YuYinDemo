class MultiHeadedAttention(Module):
  __parameters__ = []
  __buffers__ = []
  training : bool
  d_k : int
  h : int
  linear_q : __torch__.torch.nn.quantized.dynamic.modules.linear.Linear
  linear_k : __torch__.torch.nn.quantized.dynamic.modules.linear.Linear
  linear_v : __torch__.torch.nn.quantized.dynamic.modules.linear.Linear
  linear_out : __torch__.torch.nn.quantized.dynamic.modules.linear.Linear
  dropout : __torch__.torch.nn.modules.dropout.___torch_mangle_3.Dropout
  def forward(self: __torch__.wenet.transformer.attention.___torch_mangle_22.MultiHeadedAttention,
    query: Tensor,
    key: Tensor,
    value: Tensor,
    mask: Optional[Tensor],
    pos_emb: Tensor=CONSTANTS.c0) -> Tensor:
    _0 = (self).forward_qkv(query, key, value, )
    q, k, v, = _0
    _1 = torch.matmul(q, torch.transpose(k, -2, -1))
    scores = torch.div(_1, torch.sqrt(self.d_k))
    _2 = (self).forward_attention(v, scores, mask, )
    return _2
  def forward_qkv(self: __torch__.wenet.transformer.attention.___torch_mangle_22.MultiHeadedAttention,
    query: Tensor,
    key: Tensor,
    value: Tensor) -> Tuple[Tensor, Tensor, Tensor]:
    n_batch = torch.size(query, 0)
    q = torch.view((self.linear_q).forward(query, ), [n_batch, -1, self.h, self.d_k])
    k = torch.view((self.linear_k).forward(key, ), [n_batch, -1, self.h, self.d_k])
    v = torch.view((self.linear_v).forward(value, ), [n_batch, -1, self.h, self.d_k])
    q0 = torch.transpose(q, 1, 2)
    k0 = torch.transpose(k, 1, 2)
    v0 = torch.transpose(v, 1, 2)
    return (q0, k0, v0)
  def forward_attention(self: __torch__.wenet.transformer.attention.___torch_mangle_22.MultiHeadedAttention,
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
    _3 = torch.contiguous(torch.transpose(x, 1, 2), memory_format=0)
    _4 = [n_batch, -1, torch.mul(self.h, self.d_k)]
    x0 = torch.view(_3, _4)
    return (self.linear_out).forward(x0, )
