class CTCLoss(Module):
  __parameters__ = []
  __buffers__ = []
  training : bool
  zero_infinity : bool
  reduction : Final[str] = "sum"
  blank : Final[int] = 0
  def forward(self: __torch__.torch.nn.modules.loss.CTCLoss,
    log_probs: Tensor,
    targets: Tensor,
    input_lengths: Tensor,
    target_lengths: Tensor) -> Tensor:
    _0 = __torch__.torch.nn.functional.ctc_loss
    _1 = _0(log_probs, targets, input_lengths, target_lengths, 0, "sum", self.zero_infinity, )
    return _1
class KLDivLoss(Module):
  __parameters__ = []
  __buffers__ = []
  training : bool
  log_target : bool
  reduction : Final[str] = "none"
  def forward(self: __torch__.torch.nn.modules.loss.KLDivLoss,
    input: Tensor,
    target: Tensor) -> Tensor:
    _2 = __torch__.torch.nn.functional.kl_div
    _3 = _2(input, target, None, None, "none", self.log_target, )
    return _3
