class CTC(Module):
  __parameters__ = []
  __buffers__ = []
  training : bool
  dropout_rate : float
  ctc_lo : __torch__.torch.nn.quantized.dynamic.modules.linear.Linear
  ctc_loss : __torch__.torch.nn.modules.loss.CTCLoss
  def forward(self: __torch__.wenet.transformer.ctc.___torch_mangle_27.CTC,
    hs_pad: Tensor,
    hlens: Tensor,
    ys_pad: Tensor,
    ys_lens: Tensor) -> Tensor:
    _0 = __torch__.torch.nn.functional.dropout
    _1 = self.ctc_lo
    _2 = _0(hs_pad, self.dropout_rate, True, False, )
    ys_hat = (_1).forward(_2, )
    ys_hat0 = torch.transpose(ys_hat, 0, 1)
    ys_hat1 = torch.log_softmax(ys_hat0, 2, None)
    loss = (self.ctc_loss).forward(ys_hat1, ys_pad, hlens, ys_lens, )
    loss0 = torch.div(loss, torch.size(ys_hat1, 1))
    return loss0
  def log_softmax(self: __torch__.wenet.transformer.ctc.___torch_mangle_27.CTC,
    hs_pad: Tensor) -> Tensor:
    _3 = __torch__.torch.nn.functional.log_softmax
    _4 = _3((self.ctc_lo).forward(hs_pad, ), 2, 3, None, )
    return _4
